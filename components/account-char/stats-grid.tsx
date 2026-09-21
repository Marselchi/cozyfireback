"use client";

import { useState } from "react";
import {
  PencilIcon,
  CheckIcon,
  XIcon,
  PlusIcon,
  Trash2Icon,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { StatBlock } from "@/types/account-char";

interface Props {
  stats: StatBlock[];
  system: string;
  onSave: (stats: StatBlock[]) => void;
  isSaving?: boolean;
}

function formatMod(mod?: number): string {
  if (mod === undefined) return "";
  return mod >= 0 ? `+${mod}` : String(mod);
}

const MIN_SCORE = 1;
const MAX_SCORE = 30;

export function StatsGrid({ stats, system, onSave, isSaving }: Props) {
  const isGeneric = system === "generic";
  // For non-generic systems, modifiers are always auto-derived (computed).
  const modifierIsComputed = !isGeneric;

  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState<StatBlock[]>(stats);

  function handleEdit() {
    setDraft(stats);
    setEditing(true);
  }

  function handleCancel() {
    setDraft(stats);
    setEditing(false);
  }

  function handleSave() {
    onSave(draft);
    setEditing(false);
  }

  function updateStat(index: number, field: keyof StatBlock, value: string) {
    setDraft((prev) =>
      prev.map((s, i) => {
        if (i !== index) return s;

        let parsedValue: number | string = value;

        if (field === "score" || field === "modifier") {
          let num = Number.parseInt(value, 10) || 0;
          if (field === "score") {
            num = Math.min(MAX_SCORE, Math.max(MIN_SCORE, num));
          }
          parsedValue = num;
        }

        return { ...s, [field]: parsedValue };
      }),
    );
  }

  function addStat() {
    setDraft((prev) => [
      ...prev,
      { key: `stat${prev.length + 1}`, label: "", score: 10, modifier: 0 },
    ]);
  }

  function removeStat(index: number) {
    setDraft((prev) => prev.filter((_, i) => i !== index));
  }

  if (!editing) {
    return (
      <section aria-labelledby="stats-heading">
        <div className="flex items-center justify-between mb-3">
          <h2
            id="stats-heading"
            className="text-lg font-semibold text-foreground"
          >
            Статы
          </h2>
          <Button
            variant="outline"
            size="sm"
            onClick={handleEdit}
            aria-label="Edit ability scores"
          >
            <PencilIcon className="h-4 w-4" />
            <span className="sr-only">Изменить</span>
          </Button>
        </div>
        {stats.length === 0 ? (
          <p className="text-sm text-muted-foreground">Нет статов.</p>
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
            {stats.map((stat) => (
              <div
                key={stat.key}
                className="flex flex-col items-center rounded-lg border border-input bg-card p-3 text-center shadow-sm"
              >
                <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  {stat.label || stat.key}
                </span>
                <span className="mt-1 text-2xl font-bold text-foreground">
                  {stat.score}
                </span>
                {stat.modifier !== undefined && (
                  <span className="text-sm text-muted-foreground">
                    {formatMod(stat.modifier)}
                  </span>
                )}
              </div>
            ))}
          </div>
        )}
      </section>
    );
  }

  return (
    <section aria-labelledby="stats-heading-edit">
      <div className="flex items-center justify-between mb-3">
        <h2
          id="stats-heading-edit"
          className="text-lg font-semibold text-foreground"
        >
          Статы
        </h2>
      </div>
      <div className="space-y-3">
        {draft.map((stat, i) => (
          <div key={i} className="flex items-end gap-2">
            {/* Label — locked for non-generic systems */}
            <div className="flex-1 min-w-0 space-y-1">
              <Label htmlFor={`stat-label-${i}`}>Название</Label>
              <Input
                id={`stat-label-${i}`}
                value={stat.label}
                onChange={(e) => updateStat(i, "label", e.target.value)}
                placeholder="Сила"
                readOnly={!isGeneric}
                disabled={!isGeneric}
                className={!isGeneric ? "opacity-60 cursor-default" : ""}
              />
            </div>
            {/* Key — only shown for generic */}
            {isGeneric && (
              <div className="w-24 space-y-1">
                <Label htmlFor={`stat-key-${i}`}>Ключ</Label>
                <Input
                  id={`stat-key-${i}`}
                  value={stat.key}
                  onChange={(e) => updateStat(i, "key", e.target.value)}
                  placeholder="str"
                />
              </div>
            )}
            {/* Score — always editable */}
            <div className="w-20 space-y-1">
              <Label htmlFor={`stat-score-${i}`}>Значение</Label>
              <Input
                id={`stat-score-${i}`}
                type="number"
                min={MIN_SCORE}
                max={MAX_SCORE}
                value={stat.score}
                onChange={(e) => updateStat(i, "score", e.target.value)}
              />
            </div>
            {/* Modifier — read-only when computed by system rules */}
            <div className="w-20 space-y-1">
              <Label htmlFor={`stat-mod-${i}`}>Модификатор</Label>
              <Input
                id={`stat-mod-${i}`}
                type="number"
                value={stat.modifier ?? ""}
                onChange={(e) => updateStat(i, "modifier", e.target.value)}
                readOnly={modifierIsComputed}
                disabled={modifierIsComputed}
                className={
                  modifierIsComputed ? "opacity-60 cursor-default" : ""
                }
                aria-label={
                  modifierIsComputed ? "Modifier (auto-derived)" : "Modifier"
                }
              />
            </div>
            {/* Remove — only for generic */}
            {isGeneric && (
              <Button
                type="button"
                variant="ghost"
                size="icon"
                onClick={() => removeStat(i)}
                aria-label={`Remove ${stat.label || stat.key} stat`}
              >
                <Trash2Icon className="h-4 w-4 text-destructive" />
              </Button>
            )}
          </div>
        ))}
        {/* Add row — only for generic */}
        {isGeneric && (
          <Button type="button" variant="outline" size="sm" onClick={addStat}>
            <PlusIcon className="h-4 w-4" />
            Добавить стат
          </Button>
        )}
      </div>
      <div className="flex gap-2 mt-4">
        <Button size="sm" onClick={handleSave} disabled={isSaving}>
          <CheckIcon className="h-4 w-4" />
          Сохранить
        </Button>
        <Button variant="ghost" size="sm" onClick={handleCancel}>
          <XIcon className="h-4 w-4" />
          Отменить
        </Button>
      </div>
    </section>
  );
}
