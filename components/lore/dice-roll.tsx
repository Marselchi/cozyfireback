"use client";

import { useCallback, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Check, Dices, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Separator } from "@/components/ui/separator";
import { D20Canvas } from "@/components/lore/d20-canvas";
import { useRoomId } from "@/lib/room-utils";
import { rollCheck } from "@/server/blocks/actions";
import { unwrapResult } from "@/lib/utils/unwrap-result";

export type RollResult = {
  id: string;
  skill: string;
  rollValue: number;
  modifier: number;
  threshold: number;
  passed: boolean;
};

export function DiceRoll({
  open,
  onOpenChange,
  rollId,
  onResult,
  onError,
}: Readonly<{
  open: boolean;
  onOpenChange: (open: boolean) => void;
  rollId: number;
  onResult: (outcome: boolean) => void;
  onError?: (message: string) => void;
}>) {
  const [rolling, setRolling] = useState(false);
  const [settled, setSettled] = useState(false);
  const [result, setResult] = useState<RollResult | null>(null);
  const roomId = useRoomId();

  const mutation = useMutation({
    mutationFn: async () => unwrapResult(await rollCheck(roomId, rollId)),
    onSuccess: (data) => {
      setResult(data);
    },
    onError: (error) => {
      setRolling(false);
      onError?.(
        error instanceof Error ? error.message : "Не удалось выполнить бросок.",
      );
    },
  });

  const handleOpenChange = (next: boolean) => {
    if (!next && result && onResult) {
      onResult(result.passed);
    }
    onOpenChange(next);
    if (next) {
      // Сброс состояния при открытии диалога
      setResult(null);
      setSettled(false);
      setRolling(false);
      mutation.reset();
    }
  };

  function handleRoll() {
    if (mutation.isPending || result) return;
    setSettled(false);
    setRolling(true);
    mutation.mutate();
  }

  const handleSettled = useCallback(() => {
    setRolling(false);
    setSettled(true);
  }, []);

  const hasResult = Boolean(result && settled);

  function handleActionClick() {
    if (result) {
      onResult(result.passed);
      handleOpenChange(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="overflow-hidden border-border/70 bg-card p-0 shadow-2xl shadow-primary/10 sm:max-w-md">
        <div
          className={`h-1 w-full transition-colors ${
            hasResult
              ? result?.passed
                ? "bg-primary"
                : "bg-destructive"
              : "bg-transparent"
          }`}
        />
        <div className="p-6">
          <DialogHeader>
            <DialogTitle className="font-serif text-3xl tracking-tight text-center">
              У вас достаточно знаний?
            </DialogTitle>
          </DialogHeader>
          <div className="flex flex-col items-center gap-5 py-7">
            {open && (
              <D20Canvas
                rolling={rolling}
                value={result?.rollValue}
                onSettled={handleSettled}
              />
            )}
            {rolling && !settled && (
              <p className="animate-pulse text-xs uppercase tracking-[0.22em] text-muted-foreground">
                Получаем ответ вселенной...
              </p>
            )}
            {!rolling && !result && (
              <Button
                onClick={handleRoll}
                size="lg"
                className="min-w-40 rounded-xl"
              >
                Кинуть <Dices data-icon="inline-end" />
              </Button>
            )}
            {hasResult && result && (
              <div className="w-full">
                <div className="flex items-center justify-center gap-3 font-mono text-lg">
                  <span className="text-foreground">{result.rollValue}</span>
                  <span className="text-muted-foreground">
                    + {result.modifier}
                  </span>
                  <span className="text-muted-foreground">=</span>
                  <strong className="text-2xl">
                    {result.rollValue + result.modifier}
                  </strong>
                </div>
                <div className="mt-3 flex items-center justify-center">
                  Проверка: {result.skill}
                </div>
                <div className="mt-3 flex items-center justify-center gap-2 text-sm text-muted-foreground">
                  <span>Сложность</span>
                  <Separator
                    orientation="vertical"
                    className="h-4 border pt-6"
                  />
                  <span className="font-mono text-foreground">
                    {result.threshold}
                  </span>
                </div>
              </div>
            )}
          </div>
          {hasResult && result && (
            <div
              className={`rounded-xl border p-4 ${
                result.passed
                  ? "border-primary/30 bg-primary/10"
                  : "border-destructive/30 bg-destructive/10"
              }`}
            >
              <div className="mb-2 flex items-center justify-center gap-2 font-semibold">
                {result.passed ? (
                  <Check className="size-4 text-primary" />
                ) : (
                  <X className="size-4 text-destructive" />
                )}
                <span>{result.passed ? "Успешно!" : "Вам не повезло"}</span>
              </div>
              <Button
                onClick={handleActionClick}
                className={`w-full ${
                  result.passed
                    ? "bg-primary hover:bg-primary/90"
                    : "bg-destructive hover:bg-destructive/90"
                }`}
              >
                {result.passed ? "Раскрыть" : "Закрыть"}
              </Button>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
