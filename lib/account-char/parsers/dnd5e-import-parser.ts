/**
 * D&D 5e import parser.
 *
 * Pure function — no React, no fetch. Takes raw unknown JSON and returns a
 * CharacterCreatePayload (Character without `id` or `version`).
 *
 * Adding a second system's parser means creating a new file that exports the
 * same signature: (raw: unknown) => CharacterCreatePayload
 * and registering it in the PARSER_REGISTRY below.
 */

import { z } from "zod";
import { abilityModifier, skillModifier } from "../derive/dnd5e";
import { CharacterCreatePayload, StatBlock, Skill } from "@/types/account-char";

// ── Raw JSON schemas (validate before touching) ─────────────────────────────

const RawStatSchema = z.object({
  score: z.number().int(),
  modifier: z.number().int().optional(),
});

const RawSkillSchema = z.object({
  baseStat: z.string(),
  name: z.string(),
  isProf: z.number().int().optional(),
});

const RawInnerSchema = z.object({
  name: z.object({ value: z.string() }),
  info: z.object({
    charClass: z.object({ value: z.string() }),
    level: z.object({ value: z.number().int() }),
    race: z.object({ value: z.string() }),
  }),
  stats: z.record(z.string(), RawStatSchema),
  skills: z.record(z.string(), RawSkillSchema),
});

const RawOuterSchema = z.object({
  data: z.string(),
});

// ── Stat label map (D&D 5e specific — lives only in the parser) ─────────────

const STAT_LABELS: Record<string, string> = {
  str: "Сила",
  dex: "Ловкость",
  con: "Телосложение",
  int: "Интеллект",
  wis: "Мудрость",
  cha: "Харизма",
};

const SKILL_LABELS: Record<string, string> = {
  acrobatics: "Акробатика",
  animalHandling: "Уход за животными",
  arcana: "Магия",
  athletics: "Атлетика",
  deception: "Обман",
  history: "История",
  insight: "Проницательность",
  intimidation: "Запугивание",
  investigation: "Расследование",
  medicine: "Медицина",
  nature: "Природа",
  perception: "Восприятие",
  performance: "Выступление",
  persuasion: "Убеждение",
  religion: "Религия",
  sleightOfHand: "Ловкость рук",
  stealth: "Скрытность",
  survival: "Выживание",
};

const toCamelCase = (value: string): string => {
  return value
    .trim()
    .replace(/[^a-zA-Z0-9]+(.)?/g, (_, char: string) =>
      char ? char.toUpperCase() : "",
    )
    .replace(/^./, (char) => char.toLowerCase());
};

// ── Main parser ──────────────────────────────────────────────────────────────

export type ParseResult =
  | { ok: true; character: CharacterCreatePayload }
  | { ok: false; error: string };

export function parseDnd5eImport(raw: unknown, roomId: string): ParseResult {
  // 1. Validate outer wrapper
  const outerResult = RawOuterSchema.safeParse(raw);
  if (!outerResult.success) {
    return {
      ok: false,
      error: 'Expected an object with a "data" string field.',
    };
  }

  // 2. Parse inner JSON string
  let innerJson: unknown;
  try {
    innerJson = JSON.parse(outerResult.data.data);
  } catch {
    return {
      ok: false,
      error: 'The "data" field could not be parsed as JSON.',
    };
  }

  // 3. Validate inner shape
  const innerResult = RawInnerSchema.safeParse(innerJson);
  if (!innerResult.success) {
    const issues = innerResult.error.issues.map((i) => i.message).join("; ");
    return {
      ok: false,
      error: `Import data is missing required fields: ${issues}`,
    };
  }

  const d = innerResult.data;
  const level = d.info.level.value;

  // 4. Map stats
  const stats: StatBlock[] = Object.entries(d.stats).map(([key, rawStat]) => ({
    key: toCamelCase(key),
    label: STAT_LABELS[key] ?? key,
    score: rawStat.score,
    modifier: abilityModifier(rawStat.score),
  }));

  // Build a quick lookup for score by stat key (for skill modifier derivation)
  const scoreByKey: Record<string, number> = {};
  for (const s of stats) scoreByKey[s.key] = s.score;

  // 5. Map skills — proficiency: absent/undefined → 0, isProf:1 → 1, isProf:2 → 2
  const skills: Skill[] = Object.entries(d.skills).map(([key, rawSkill]) => {
    const proficiency = rawSkill.isProf ?? 0;
    const score = scoreByKey[rawSkill.baseStat] ?? 10;
    const skillKey = toCamelCase(rawSkill.name ?? key);

    return {
      key: toCamelCase(key),
      label: SKILL_LABELS[skillKey] ?? rawSkill.name ?? key,
      statKey: rawSkill.baseStat,
      proficiency,
      modifier: skillModifier(score, proficiency, level),
    };
  });

  // 6. Assemble payload
  const character: CharacterCreatePayload = {
    roomId,
    system: "DND_5E",
    name: d.name.value || "Unnamed",
    level,
    class: d.info.charClass.value || "",
    race: d.info.race.value || "",
    stats,
    skills,
    origin: { source: "import", importSystem: "DND_5E" },
  };

  return { ok: true, character };
}

// ── Parser registry (extensible for future systems) ─────────────────────────

export type ParserFn = (raw: unknown, roomId: string) => ParseResult;

export const PARSER_REGISTRY: Record<string, ParserFn> = {
  DND_5E: parseDnd5eImport,
};
