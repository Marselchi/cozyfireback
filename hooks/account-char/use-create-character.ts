"use client";

import { ApiError } from "@/lib/auth/apiError";
import { unwrapResult } from "@/lib/utils/unwrap-result";
import { createCharacter } from "@/server/account-char/actions";
import { Character, CharacterCreatePayload } from "@/types/account-char";
import { useMutation, useQueryClient } from "@tanstack/react-query";

export function useCreateCharacter(roomId: string) {
  const qc = useQueryClient();
  return useMutation<Character, ApiError, CharacterCreatePayload>({
    mutationFn: async (payload) =>
      unwrapResult(await createCharacter(roomId, payload)),
    onSuccess: (character) => {
      qc.setQueryData(["character-self", roomId], character);
    },
  });
}