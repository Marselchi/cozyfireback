"use client";

import { useState, useEffect } from "react";
import { PlusIcon, Trash2Icon, MinusIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import {
  abilityModifier,
  skillModifier,
} from "@/lib/account-char/derive/dnd5e";
import {
  StatBlock,
  Skill,
  SupportedSystem,
  CharacterCreatePayload,
  SUPPORTED_SYSTEMS,
} from "@/types/account-char";

// ── D&D 5e starter preset ─────────────────────────────────────────────────────

const DND5E_STARTER_STATS: StatBlock[] = [
  { key: "str", label: "Сила", score: 10, modifier: 0 },
  { key: "dex", label: "Ловкость", score: 10, modifier: 0 },
  { key: "con", label: "Телосложение", score: 10, modifier: 0 },
  { key: "int", label: "Интеллект", score: 10, modifier: 0 },
  { key: "wis", label: "Мудрость", score: 10, modifier: 0 },
  { key: "cha", label: "Харизма", score: 10, modifier: 0 },
];

const DND5E_STARTER_SKILLS: Skill[] = [
  {
    key: "acrobatics",
    label: "Акробатика",
    statKey: "dex",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "animalHandling",
    label: "Уход за животными",
    statKey: "wis",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "arcana",
    label: "Магия",
    statKey: "int",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "athletics",
    label: "Атлетика",
    statKey: "str",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "deception",
    label: "Обман",
    statKey: "cha",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "history",
    label: "История",
    statKey: "int",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "insight",
    label: "Проницательность",
    statKey: "wis",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "intimidation",
    label: "Запугивание",
    statKey: "cha",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "investigation",
    label: "Расследование",
    statKey: "int",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "medicine",
    label: "Медицина",
    statKey: "wis",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "nature",
    label: "Природа",
    statKey: "int",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "perception",
    label: "Восприятие",
    statKey: "wis",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "performance",
    label: "Выступление",
    statKey: "cha",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "persuasion",
    label: "Убеждение",
    statKey: "cha",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "religion",
    label: "Религия",
    statKey: "int",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "sleightOfHand",
    label: "Ловкость рук",
    statKey: "dex",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "stealth",
    label: "Скрытность",
    statKey: "dex",
    proficiency: 0,
    modifier: 0,
  },
  {
    key: "survival",
    label: "Выживание",
    statKey: "wis",
    proficiency: 0,
    modifier: 0,
  },
];
// ── Helpers ───────────────────────────────────────────────────────────────────

const PROFICIENCY_LABELS: Record<number, string> = {
  0: "Нет",
  1: "Владение",
  2: "Компетентность",
};

/** Returns true when the system has fixed, schema-defined fields (not free-form). */
function isSystemLocked(system: SupportedSystem) {
  //return system !== "generic";
  return true;
}

interface Props {
  roomId: string;
  onSubmit: (payload: CharacterCreatePayload) => void;
  isSubmitting?: boolean;
  submitError?: string | null;
}

export function ManualCreateForm({
  roomId,
  onSubmit,
  isSubmitting,
  submitError,
}: Props) {
  const [name, setName] = useState("");
  const [level, setLevel] = useState("1");
  const [cls, setCls] = useState("");
  const [race, setRace] = useState("");
  const [system, setSystem] = useState<SupportedSystem>("DND_5E");
  const [stats, setStats] = useState<StatBlock[]>([]);
  const [skills, setSkills] = useState<Skill[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Auto-apply / clear the preset whenever the system changes.
  useEffect(() => {
    if (system === "DND_5E") {
      setStats(DND5E_STARTER_STATS);
      setSkills(DND5E_STARTER_SKILLS);
    } else {
      // Reset to empty for generic (or any other future system without a preset).
      setStats([]);
      setSkills([]);
    }
  }, [system]);

  const locked = isSystemLocked(system);

  // ── stat helpers ──────────────────────────────────────────────────────────

  function addStat() {
    if (locked) return;
    setStats((prev) => [
      ...prev,
      { key: `stat${prev.length + 1}`, label: "", score: 10, modifier: 0 },
    ]);
  }

  function removeStat(index: number) {
    if (locked) return;
    setStats((prev) => prev.filter((_, i) => i !== index));
  }

  function updateStat(index: number, field: keyof StatBlock, value: string) {
    setStats((prev) =>
      prev.map((s, i) => {
        if (i !== index) return s;
        if (field === "score") {
          const score = parseInt(value, 10) || 0;
          const modifier =
            system === "DND_5E" ? abilityModifier(score) : s.modifier;
          // For dnd5e, also recompute all skill modifiers that link to this stat.
          if (system === "DND_5E") {
            const lvl = parseInt(level, 10) || 1;
            setSkills((prevSkills) =>
              prevSkills.map((sk) =>
                sk.statKey === s.key
                  ? {
                      ...sk,
                      modifier: skillModifier(score, sk.proficiency, lvl),
                    }
                  : sk,
              ),
            );
          }
          return { ...s, score, modifier };
        }
        if (field === "modifier" && !locked)
          return { ...s, modifier: parseInt(value, 10) || 0 };
        // For locked systems, skip label/key changes.
        if (locked && (field === "label" || field === "key")) return s;
        return { ...s, [field]: value };
      }),
    );
  }

  // ── skill helpers ─────────────────────────────────────────────────────────

  function addSkill() {
    if (locked) return;
    setSkills((prev) => [
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
    if (locked) return;
    setSkills((prev) => prev.filter((_, i) => i !== index));
  }

  function updateSkill(
    index: number,
    field: keyof Skill,
    value: string | number,
  ) {
    setSkills((prev) =>
      prev.map((sk, i) => {
        if (i !== index) return sk;
        // Block label/key edits for locked systems.
        if (locked && (field === "label" || field === "key")) return sk;
        const updated = { ...sk, [field]: value };
        // Auto-recompute modifier for dnd5e whenever statKey or proficiency changes.
        if (
          system === "DND_5E" &&
          (field === "statKey" || field === "proficiency")
        ) {
          const linkedStat = stats.find((s) => s.key === updated.statKey);
          const lvl = parseInt(level, 10) || 1;
          if (linkedStat) {
            updated.modifier = skillModifier(
              linkedStat.score,
              Number(updated.proficiency),
              lvl,
            );
          }
        }
        return updated;
      }),
    );
  }

  function stepProficiency(index: number, delta: number) {
    const sk = skills[index];
    if (!sk) return;
    const next = Math.max(0, Math.min(2, sk.proficiency + delta));
    updateSkill(index, "proficiency", next);
  }

  // ── submit ────────────────────────────────────────────────────────────────

  function handleSubmit(e: React.SubmitEvent) {
    e.preventDefault();
    const newErrors: Record<string, string> = {};
    if (!name.trim()) newErrors.name = "Имя необходимо.";
    const parsedLevel = parseInt(level, 10);
    if (isNaN(parsedLevel) || parsedLevel < 1)
      newErrors.level = "Уровень не ниже первого.";
    setErrors(newErrors);
    if (Object.keys(newErrors).length > 0) return;

    onSubmit({
      roomId,
      system,
      name: name.trim(),
      level: parsedLevel,
      class: cls.trim(),
      race: race.trim(),
      stats,
      skills,
      origin: { source: "manual" },
    });
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-8">
      {/* Identity */}
      <fieldset className="space-y-4">
        <legend className="text-base font-semibold text-foreground">
          Личность
        </legend>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="space-y-1.5">
            <Label htmlFor="manual-name">
              Имя{" "}
              <span aria-hidden="true" className="text-destructive">
                *
              </span>
            </Label>
            <Input
              id="manual-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              aria-describedby={errors.name ? "manual-name-error" : undefined}
              aria-invalid={!!errors.name}
            />
            {errors.name && (
              <p
                id="manual-name-error"
                role="alert"
                className="text-xs text-destructive"
              >
                {errors.name}
              </p>
            )}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="manual-level">
              Уровень{" "}
              <span aria-hidden="true" className="text-destructive">
                *
              </span>
            </Label>
            <Input
              id="manual-level"
              type="number"
              min={1}
              max={20}
              value={level}
              onChange={(e) => setLevel(e.target.value)}
              aria-describedby={errors.level ? "manual-level-error" : undefined}
              aria-invalid={!!errors.level}
            />
            {errors.level && (
              <p
                id="manual-level-error"
                role="alert"
                className="text-xs text-destructive"
              >
                {errors.level}
              </p>
            )}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="manual-class">Класс</Label>
            <Input
              id="manual-class"
              value={cls}
              onChange={(e) => setCls(e.target.value)}
              placeholder="Плут"
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="manual-race">Раса</Label>
            <Input
              id="manual-race"
              value={race}
              onChange={(e) => setRace(e.target.value)}
              placeholder="Эльф"
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="manual-system">Система</Label>
            <Select
              value={system}
              onValueChange={(v) => setSystem(v as SupportedSystem)}
            >
              <SelectTrigger id="manual-system">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {SUPPORTED_SYSTEMS.map((s) => (
                  <SelectItem key={s.value} value={s.value}>
                    {s.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
      </fieldset>

      <Separator />

      {/* Ability Scores */}
      <fieldset className="space-y-4">
        <legend className="text-base font-semibold text-foreground">
          Статы
        </legend>
        {stats.length === 0 && (
          <p className="text-sm text-muted-foreground">Статы не указаны</p>
        )}
        {stats.map((stat, i) => (
          <div
            key={stat.key}
            className="grid gap-2 items-end"
            style={{
              gridTemplateColumns: locked
                ? "1fr 6rem 5rem"
                : "1fr 4rem 6rem 5rem 2.5rem",
            }}
          >
            {/* Label — locked systems: read-only text, generic: editable */}
            <div className="space-y-1">
              {i === 0 && <Label>Название</Label>}
              <Input
                value={stat.label}
                onChange={(e) => updateStat(i, "label", e.target.value)}
                readOnly={locked}
                className={
                  locked ? "bg-muted/40 cursor-default select-none" : ""
                }
                placeholder="Strength"
                aria-label={`Stat ${i + 1} label`}
              />
            </div>

            {/* Key — only shown for generic */}
            {!locked && (
              <div className="space-y-1">
                {i === 0 && <Label>Key</Label>}
                <Input
                  value={stat.key}
                  onChange={(e) => updateStat(i, "key", e.target.value)}
                  placeholder="str"
                  aria-label={`Stat ${i + 1} key`}
                />
              </div>
            )}

            {/* Score */}
            <div className="space-y-1">
              {i === 0 && <Label>Значение</Label>}
              <Input
                type="number"
                value={stat.score}
                onChange={(e) => updateStat(i, "score", e.target.value)}
                aria-label={`Stat ${i + 1} score`}
              />
            </div>

            {/* Modifier — auto-derived for dnd5e, so read-only + no native spinners */}
            <div className="space-y-1">
              {i === 0 && (
                <Label className="flex items-center gap-1">
                  Модификатор
                  {locked && (
                    <span className="text-muted-foreground text-[10px] font-normal">
                      (авто)
                    </span>
                  )}
                </Label>
              )}
              <Input
                // Use type="text" to suppress the native number up/down arrows when locked.
                type={locked ? "text" : "number"}
                inputMode="numeric"
                value={
                  locked
                    ? stat.modifier && stat.modifier >= 0
                      ? `+${stat.modifier}`
                      : String(stat.modifier)
                    : stat.modifier
                }
                onChange={(e) =>
                  !locked && updateStat(i, "modifier", e.target.value)
                }
                readOnly={locked}
                className={
                  locked
                    ? "bg-muted/40 cursor-default text-center tabular-nums font-medium"
                    : ""
                }
                aria-label={`Stat ${i + 1} modifier`}
              />
            </div>

            {/* Remove — only for generic */}
            {!locked && (
              <div className="flex justify-end">
                {i === 0 && <div className="h-5" />}
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={() => removeStat(i)}
                  aria-label={`Remove ${stat.label || stat.key}`}
                >
                  <Trash2Icon className="h-4 w-4 text-destructive" />
                </Button>
              </div>
            )}
          </div>
        ))}

        {/* Add row — only for generic */}
        {!locked && (
          <Button type="button" variant="outline" size="sm" onClick={addStat}>
            <PlusIcon className="h-4 w-4 mr-1" />
            Добавить значение
          </Button>
        )}
      </fieldset>

      <Separator />

      {/* Skills */}
      <fieldset className="space-y-4">
        <legend className="text-base font-semibold text-foreground">
          Навыки
        </legend>
        {skills.length === 0 && (
          <p className="text-sm text-muted-foreground">Навыки не указаны</p>
        )}
        {skills.map((skill, i) => (
          <div
            key={skill.key}
            className="grid gap-2 items-end"
            style={{
              gridTemplateColumns: locked
                ? "minmax(10rem, 1fr) 12rem 5rem"
                : "minmax(10rem, 1fr) 4rem 3.5rem 10rem 5rem 2.5rem",
            }}
          >
            {/* Label */}
            <div className="space-y-1">
              {i === 0 && <Label>Название</Label>}
              <Input
                value={skill.label}
                onChange={(e) => updateSkill(i, "label", e.target.value)}
                readOnly={locked}
                className={
                  locked ? "bg-muted/40 cursor-default select-none" : ""
                }
                placeholder="Акробатика"
                aria-label={`Skill ${i + 1} label`}
              />
            </div>

            {/* Key — generic only */}
            {!locked && (
              <div className="space-y-1">
                {i === 0 && <Label>Ключ</Label>}
                <Input
                  value={skill.key}
                  onChange={(e) => updateSkill(i, "key", e.target.value)}
                  placeholder="acrobatics"
                  aria-label={`Skill ${i + 1} key`}
                />
              </div>
            )}

            {/* Stat key (linked ability) — generic: free text, locked: read-only */}
            {!locked && (
              <div className="space-y-1">
                {i === 0 && <Label>Стат</Label>}
                <Input
                  value={skill.statKey}
                  onChange={(e) => updateSkill(i, "statKey", e.target.value)}
                  placeholder="dex"
                  list="mcf-stat-keys"
                  aria-label={`Skill ${i + 1} linked stat`}
                />
                <datalist id="mcf-stat-keys">
                  {stats.map((s) => (
                    <option key={s.key} value={s.key} />
                  ))}
                </datalist>
              </div>
            )}

            {/* Proficiency stepper — always editable */}
            <div className="space-y-1">
              {i === 0 && <Label>Мастерство</Label>}
              <div className="flex items-center gap-1">
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  className="h-8 w-8 shrink-0"
                  onClick={() => stepProficiency(i, -1)}
                  disabled={skill.proficiency <= 0}
                  aria-label="Decrease proficiency"
                >
                  <MinusIcon className="h-3 w-3" />
                </Button>
                <span className="flex-1 text-center text-xs font-medium truncate">
                  {PROFICIENCY_LABELS[skill.proficiency] ??
                    `Тир ${skill.proficiency}`}
                </span>
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  className="h-8 w-8 shrink-0"
                  onClick={() => stepProficiency(i, 1)}
                  disabled={skill.proficiency >= 2}
                  aria-label="Increase proficiency"
                >
                  <PlusIcon className="h-3 w-3" />
                </Button>
              </div>
            </div>

            {/* Modifier — locked systems: auto-derived, no spinners */}
            <div className="space-y-1">
              {i === 0 && (
                <Label className="flex items-center gap-1">
                  Модификатор
                  {locked && (
                    <span className="text-muted-foreground text-[10px] font-normal">
                      (авто)
                    </span>
                  )}
                </Label>
              )}
              <Input
                type={locked ? "text" : "number"}
                inputMode="numeric"
                value={
                  locked
                    ? skill.modifier >= 0
                      ? `+${skill.modifier}`
                      : String(skill.modifier)
                    : skill.modifier
                }
                onChange={(e) =>
                  !locked &&
                  updateSkill(i, "modifier", parseInt(e.target.value, 10) || 0)
                }
                readOnly={locked}
                className={
                  locked
                    ? "bg-muted/40 cursor-default text-center tabular-nums font-medium"
                    : ""
                }
                aria-label={`Skill ${i + 1} modifier`}
              />
            </div>

            {/* Remove — generic only */}
            {!locked && (
              <div className="flex justify-end">
                {i === 0 && <div className="h-5" />}
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={() => removeSkill(i)}
                  aria-label={`Remove ${skill.label || skill.key}`}
                >
                  <Trash2Icon className="h-4 w-4 text-destructive" />
                </Button>
              </div>
            )}
          </div>
        ))}

        {/* Add row — generic only */}
        {!locked && (
          <Button type="button" variant="outline" size="sm" onClick={addSkill}>
            <PlusIcon className="h-4 w-4 mr-1" />
            Добавить навык
          </Button>
        )}
      </fieldset>

      <Separator />

      {submitError && (
        <p role="alert" className="text-sm text-destructive">
          {submitError}
        </p>
      )}

      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Creating…" : "Create character"}
      </Button>
    </form>
  );
}
