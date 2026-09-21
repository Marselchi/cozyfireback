"use client";

import { ApiError } from "@/lib/auth/apiError";
import { getCharacter } from "@/server/account-char/actions";
import { Character } from "@/types/account-char";
import { useQuery } from "@tanstack/react-query";

export function useCharacter(
  roomId: string,
  characterId: string,
  options?: { enabled?: boolean },
) {
  return useQuery<Character, ApiError>({
    queryKey: ["character", characterId],
    queryFn: () => getCharacter(roomId, characterId),
    enabled: Boolean(roomId && characterId) && (options?.enabled ?? true),
  });
}
