"use client";

import { useState } from "react";
import {
  PencilIcon,
  CheckIcon,
  XIcon,
  PlusIcon,
  Trash2Icon,
  MinusIcon,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Skill, StatBlock } from "@/types/account-char";

interface Props {
  skills: Skill[];
  stats: StatBlock[];
  system: string;
  onSave: (skills: Skill[]) => void;
  isSaving?: boolean;
}

const PROFICIENCY_LABELS: Record<number, string> = {
  0: "—",
  1: "Владение",
  2: "Компетенция",
};

const MAX_PROFICIENCY = 2;

function formatMod(mod: number): string {
  return mod >= 0 ? `+${mod}` : String(mod);
}

export function SkillsList({ skills, stats, system, onSave, isSaving }: Props) {
  const isGeneric = system === "generic";
  // Modifier is computed by system rules for any non-generic system.
  const modifierIsComputed = !isGeneric;

  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState<Skill[]>(skills);

  function handleEdit() {
    setDraft(skills);
    setEditing(true);
  }

  function handleCancel() {
    setDraft(skills);
    setEditing(false);
  }

  function handleSave() {
    onSave(draft);
    setEditing(false);
  }

  function updateSkill(
    index: number,
    field: keyof Skill,
    value: string | number,
  ) {
    setDraft((prev) =>
      prev.map((sk, i) => (i === index ? { ...sk, [field]: value } : sk)),
    );
  }

  function stepProficiency(index: number, delta: number) {
    setDraft((prev) =>
      prev.map((sk, i) =>
        i === index
          ? {
              ...sk,
              proficiency: Math.min(
                MAX_PROFICIENCY,
                Math.max(0, sk.proficiency + delta),
              ),
            }
          : sk,
      ),
    );
  }

  function addSkill() {
    setDraft((prev) => [
      ...prev,
      {
        key: `skill${prev.length + 1}`,
        label: "",
        statKey: stats[0]?.key ?? "",
        proficiency: 0,
        modifier: 0,
      },
    ]);
  }

  function removeSkill(index: number) {
    setDraft((prev) => prev.filter((_, i) => i !== index));
  }

  if (!editing) {
    return (
      <section aria-labelledby="skills-heading">
        <div className="flex items-center justify-between mb-3">
          <h2
            id="skills-heading"
            className="text-lg font-semibold text-foreground"
          >
            Навыки
          </h2>
          <Button
            variant="outline"
            size="sm"
            onClick={handleEdit}
            aria-label="Edit skills"
          >
            <PencilIcon className="h-4 w-4" />
            <span className="sr-only">Изменить</span>
          </Button>
        </div>
        {skills.length === 0 ? (
          <p className="text-sm text-muted-foreground">Навыки не определены</p>
        ) : (
          <div className="rounded-lg border border-input divide-y divide-input/60">
            {skills.map((skill) => (
              <div
                key={skill.key}
                className="flex items-center justify-between px-4 py-2"
              >
                <div className="flex items-center gap-3">
                  <span className="w-8 text-center text-sm font-mono font-semibold text-foreground">
                    {formatMod(skill.modifier)}
                  </span>
                  <span className="text-sm text-foreground">
                    {skill.label || skill.key}
                  </span>
                  {/* Only show stat key in read view for generic (label is enough for named systems) */}
                  {isGeneric && (
                    <span className="text-xs text-muted-foreground uppercase">
                      {skill.statKey}
                    </span>
                  )}
                </div>
                {skill.proficiency > 0 && (
                  <Badge
                    variant="secondary"
                    className="text-xs pointer-events-none"
                  >
                    {PROFICIENCY_LABELS[skill.proficiency] ??
                      `Тир ${skill.proficiency}`}
                  </Badge>
                )}
              </div>
            ))}
          </div>
        )}
      </section>
    );
  }

  return (
    <section aria-labelledby="skills-heading-edit">
      <div className="flex items-center justify-between mb-3">
        <h2
          id="skills-heading-edit"
          className="text-lg font-semibold text-foreground"
        >
          Навыки
        </h2>
      </div>
      <div className="space-y-3">
        {draft.map((skill, i) => (
          <div key={i} className="flex items-end gap-2">
            {/* Label — locked for non-generic systems */}
            <div className="flex-1 min-w-0 space-y-1">
              <Label htmlFor={`skill-label-${i}`}>Название</Label>
              <Input
                id={`skill-label-${i}`}
                value={skill.label}
                onChange={(e) => updateSkill(i, "label", e.target.value)}
                placeholder="Акробатика"
                readOnly={!isGeneric}
                className={!isGeneric ? "opacity-60 cursor-default" : ""}
              />
            </div>

            {/* Stat key — only shown for generic */}
            {isGeneric && (
              <div className="w-20 space-y-1">
                <Label htmlFor={`skill-stat-${i}`}>Характеристика</Label>
                <Input
                  id={`skill-stat-${i}`}
                  value={skill.statKey}
                  onChange={(e) => updateSkill(i, "statKey", e.target.value)}
                  placeholder="dex"
                  list="stat-keys"
                />
                <datalist id="stat-keys">
                  {stats.map((s) => (
                    <option key={s.key} value={s.key} />
                  ))}
                </datalist>
              </div>
            )}

            {/* Proficiency stepper — always user-controlled */}
            <div className="space-y-1">
              <Label>Мастерство</Label>
              <div className="flex items-center gap-1">
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  className="h-8 w-8"
                  onClick={() => stepProficiency(i, -1)}
                  disabled={skill.proficiency <= 0}
                  aria-label="Decrease proficiency"
                >
                  <MinusIcon className="h-3 w-3" />
                </Button>
                <span className="w-25 text-center text-sm font-medium">
                  {PROFICIENCY_LABELS[skill.proficiency] ??
                    `уровень ${skill.proficiency}`}
                </span>
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  className="h-8 w-8"
                  disabled={skill.proficiency >= MAX_PROFICIENCY}
                  onClick={() => stepProficiency(i, 1)}
                  aria-label="Increase proficiency"
                >
                  <PlusIcon className="h-3 w-3" />
                </Button>
              </div>
            </div>

            {/* Modifier — read-only when system computes it */}
            <div className="w-20 space-y-1">
              <Label htmlFor={`skill-mod-${i}`}>
                Модификатор
                {modifierIsComputed && (
                  <span className="text-muted-foreground ml-1 text-xs">
                    (авто)
                  </span>
                )}
              </Label>
              <Input
                id={`skill-mod-${i}`}
                type="number"
                value={skill.modifier}
                onChange={(e) =>
                  updateSkill(
                    i,
                    "modifier",
                    Number.parseInt(e.target.value, 10) || 0,
                  )
                }
                readOnly={modifierIsComputed}
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
                onClick={() => removeSkill(i)}
                aria-label={`Remove ${skill.label || skill.key} skill`}
              >
                <Trash2Icon className="h-4 w-4 text-destructive" />
              </Button>
            )}
          </div>
        ))}

        {/* Add row — only for generic */}
        {isGeneric && (
          <Button type="button" variant="outline" size="sm" onClick={addSkill}>
            <PlusIcon className="h-4 w-4" />
            Добавить скилл
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
