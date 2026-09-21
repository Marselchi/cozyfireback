"use client";

import { CheckIcon, XIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { CharacterCreatePayload } from "@/types/account-char";

interface Props {
  character: CharacterCreatePayload;
  onConfirm: () => void;
  onCancel: () => void;
  isSubmitting?: boolean;
  submitError?: string | null;
}

function formatMod(mod?: number): string {
  if (mod === undefined) return "";
  return mod >= 0 ? `+${mod}` : String(mod);
}

const PROFICIENCY_LABELS: Record<number, string> = {
  0: "—",
  1: "Владение",
  2: "Компетенция",
};

export function ImportPreview({
  character,
  onConfirm,
  onCancel,
  isSubmitting,
  submitError,
}: Readonly<Props>) {
  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-base font-semibold text-foreground">Превью</h3>
        <p className="text-sm text-muted-foreground mt-0.5">
          Проверьте поля. Только указанные ниже поля будут импортированны, все
          остальное будет проигнорированно
        </p>
      </div>

      {/* Identity */}
      <div className="rounded-lg border border-input p-4 space-y-2">
        <div className="flex flex-wrap items-baseline gap-2">
          <span className="text-xl font-bold text-foreground">
            {character.name}
          </span>
          <Badge variant="secondary">Уровень {character.level}</Badge>
          {character.system && (
            <Badge variant="default" className="text-xs">
              {character.system}
            </Badge>
          )}
        </div>
        <div className="text-sm text-muted-foreground space-x-2">
          {character.race && (
            <span>
              Раса:{" "}
              <strong className="text-foreground">{character.race}</strong>
            </span>
          )}
          {character.class && (
            <span>
              Класс:{" "}
              <strong className="text-foreground">{character.class}</strong>
            </span>
          )}
        </div>
      </div>

      {/* Stats */}
      {character.stats.length > 0 && (
        <div>
          <p className="text-sm font-medium text-foreground mb-2">Статы</p>
          <div className="grid grid-cols-3 gap-2 sm:grid-cols-6">
            {character.stats.map((s) => (
              <div
                key={s.key}
                className="flex flex-col items-center rounded border border-input bg-muted/40 px-2 py-2 text-center"
              >
                <span className="text-xs font-semibold uppercase text-muted-foreground">
                  {s.label || s.key}
                </span>
                <span className="text-lg font-bold text-foreground">
                  {s.score}
                </span>
                {s.modifier !== undefined && (
                  <span className="text-xs text-muted-foreground">
                    {formatMod(s.modifier)}
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Skills */}
      {character.skills.length > 0 && (
        <div>
          <p className="text-sm font-medium text-foreground mb-2">Навыки</p>
          <div className="rounded-lg border border-input divide-y divide-input/60">
            {character.skills.map((sk) => (
              <div
                key={sk.key}
                className="flex items-center justify-between px-3 py-1.5"
              >
                <div className="flex items-center gap-2">
                  <span className="w-8 text-center text-xs font-mono font-semibold text-foreground">
                    {formatMod(sk.modifier)}
                  </span>
                  <span className="text-sm text-foreground">
                    {sk.label || sk.key}
                  </span>
                  <span className="text-xs text-muted-foreground uppercase">
                    {sk.statKey}
                  </span>
                </div>
                {sk.proficiency > 0 && (
                  <Badge variant="secondary" className="text-xs">
                    {PROFICIENCY_LABELS[sk.proficiency] ??
                      `Тир ${sk.proficiency}`}
                  </Badge>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      <Separator />

      {submitError && (
        <p role="alert" className="text-sm text-destructive">
          {submitError}
        </p>
      )}

      <div className="flex gap-2">
        <Button onClick={onConfirm} disabled={isSubmitting}>
          <CheckIcon className="h-4 w-4" />
          {isSubmitting ? "Сохраняю..." : "Сохранить"}
        </Button>
        <Button variant="ghost" onClick={onCancel} disabled={isSubmitting}>
          <XIcon className="h-4 w-4" />
          Назад
        </Button>
      </div>
    </div>
  );
}
