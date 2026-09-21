"use client";

import { useState } from "react";
import { useCreateCharacter } from "./use-create-character";
import { useUpdateCharacter } from "./use-update-character";
import { Character } from "@/types/account-char";
import {
  PARSER_REGISTRY,
  ParseResult,
} from "@/lib/account-char/parsers/dnd5e-import-parser";

function detectSystem(raw: unknown): string {
  return "DND_5E";
}

export type ImportStep = "idle" | "preview" | "submitting" | "done" | "error";

interface UseImportCharacterOptions {
  roomId: string;
  characterId?: string;
  isSelf?: boolean;
  onSuccess?: () => void
}

export function useImportCharacter({
  roomId,
  characterId,
  isSelf = false,
  onSuccess
}: UseImportCharacterOptions) {
  const { mutateAsync: create } = useCreateCharacter(roomId);
  const { mutateAsync: update } = useUpdateCharacter(
    roomId,
    characterId,
    isSelf,
  );

  const [step, setStep] = useState<ImportStep>("idle");
  const [parseResult, setParseResult] = useState<ParseResult | null>(null);
  const [parseError, setParseError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [savedCharacter, setSavedCharacter] = useState<Character | null>(null);

  function parseFile(raw: unknown) {
    setSubmitError(null);
    const system = detectSystem(raw);
    const parser = PARSER_REGISTRY[system];
    if (!parser) {
      setParseError(`No parser available for system "${system}".`);
      setParseResult(null);
      return;
    }
    const result = parser(raw, roomId);
    if (result.ok) {
      setParseResult(result);
      setParseError(null);
      setStep("preview");
    } else {
      setParseError(result.error);
      setParseResult(null);
      setStep("error");
    }
  }

  async function confirm() {
    if (!parseResult?.ok) return;
    setStep("submitting");
    setSubmitError(null);
    try {
      const characterData = parseResult.character;

      if (characterId) {
        await update({
          patch: {
            ...characterData,
            version: 0, // Версия пока игнорируется
          },
        });
      } else {
        const character = await create(characterData);
        setSavedCharacter(character);
      }
      setStep("done");
      onSuccess?.();
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : String(err));
      setStep("error");
    }
  }

  function reset() {
    setStep("idle");
    setParseResult(null);
    setParseError(null);
    setSubmitError(null);
    setSavedCharacter(null);
  }

  return {
    step,
    parseResult,
    parseError,
    submitError,
    savedCharacter,
    parseFile,
    confirm,
    reset,
    isUpdateMode: Boolean(characterId),
  };
}
