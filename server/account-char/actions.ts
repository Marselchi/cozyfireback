"use server";
import { getWithAuth, sendWithAuth } from "@/lib/auth/apiClient";
import { ApiError, FrontApiError, toFrontApiError } from "@/lib/auth/apiError";
import {
  Character,
  CharacterCreatePayload,
  CharacterUpdatePayload,
} from "@/types/account-char";
import "server-only";

export async function getCharacter(
  roomId: string,
  characterId: string,
): Promise<Character> {
  const [error, data] = await getWithAuth(`/account_chars/${characterId}`);

  if (error || !data) {
    throw error ?? new ApiError(404, "", `Character ${characterId} not found`);
  }

  return data;
}

export async function getCharacterSelf(
  roomId: string,
): Promise<[FrontApiError | null, Character | null]> {
  const [error, data] = await getWithAuth<Character>(
    `/account_chars/self/${roomId}`,
  );

  console.log(data?.stats);

  if (error) {
    return [toFrontApiError(error), null];
  }

  return [null, data];
}

export async function createCharacter(
  roomId: string,
  payload: CharacterCreatePayload,
): Promise<[FrontApiError | null, Character | null]> {
  const [error, data] = await sendWithAuth(`/account_chars/${roomId}`, "POST", {
    ...payload,
    origin: payload.origin.source,
  });

  if (error) {
    return [toFrontApiError(error), null];
  }

  return [null, data];
}

export async function updateCharacter(
  roomId: string,
  characterId: string,
  patch: CharacterUpdatePayload & { version: number },
): Promise<[FrontApiError | null, Character | null]> {
  const [error, data] = await sendWithAuth(
    `/account_chars/${roomId}/${characterId}`,
    "PATCH",
    {
      ...patch,
      origin: patch.origin?.source,
    },
  );

  if (error) {
    return [toFrontApiError(error), null];
  }

  return [null, data];
}
