"use client";

import { ApiError } from "@/lib/auth/apiError";
import { unwrapResult } from "@/lib/utils/unwrap-result";
import { updateCharacter } from "@/server/account-char/actions";
import { Character, CharacterUpdatePayload } from "@/types/account-char";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { characterKeys } from "./query-keys";

interface UpdateArgs {
  patch: CharacterUpdatePayload & { version: number };
}

export function useUpdateCharacter(
  roomId: string,
  characterId: string | undefined,
  isSelf: boolean,
) {
  const qc = useQueryClient();
  const queryKey = isSelf
    ? characterKeys.self(roomId)
    : characterKeys.byId(characterId ?? "");

  return useMutation<
    Character,
    ApiError,
    UpdateArgs,
    { previous: Character | undefined }
  >({
    mutationFn: async ({ patch }) => {
      if (!characterId) {
        throw new ApiError(
          400,
          "",
          "Cannot update character: no characterId resolved yet",
        );
      }
      return unwrapResult(await updateCharacter(roomId, characterId, patch));
    },

    // Optimistic update
    onMutate: async ({ patch }) => {
      await qc.cancelQueries({ queryKey });
      const previous = qc.getQueryData<Character>(queryKey);
      if (previous) {
        qc.setQueryData<Character>(queryKey, { ...previous, ...patch });
      }
      return { previous };
    },

    onError: (_err, _vars, ctx) => {
      // Roll back on error — теперь сработает и для реальных API-ошибок
      if (ctx?.previous) {
        qc.setQueryData(queryKey, ctx.previous);
      }
    },

    onSuccess: (updated) => {
      qc.setQueryData(queryKey, updated);
    },
  });
}
