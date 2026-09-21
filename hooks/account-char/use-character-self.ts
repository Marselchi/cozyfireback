"use client";

import { ApiError } from "@/lib/auth/apiError";
import { unwrapResult } from "@/lib/utils/unwrap-result";
import { getCharacterSelf } from "@/server/account-char/actions";
import { Character } from "@/types/account-char";
import { useQuery } from "@tanstack/react-query";

export function useCharacterSelf(
  roomId: string,
  options?: { enabled?: boolean },
) {
  return useQuery<Character, ApiError>({
    queryKey: ["character-self", roomId],
    queryFn: async () => unwrapResult(await getCharacterSelf(roomId)),
    enabled: Boolean(roomId) && (options?.enabled ?? true),
    refetchOnMount: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
}
