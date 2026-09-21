import { z } from "zod";

// ── Zod schemas ────────────────────────────────────────────────────────────────

export const StatBlockSchema = z.object({
  key: z.string().min(1),
  label: z.string().min(1),
  score: z.number().int(),
  modifier: z.number().int().optional(),
});

export const SkillSchema = z.object({
  key: z.string().min(1),
  label: z.string().min(1),
  statKey: z.string().min(1),
  proficiency: z.number().int().min(0),
  modifier: z.number().int(),
});

export const CharacterOriginSchema = z.discriminatedUnion("source", [
  z.object({ source: z.literal("manual") }),
  z.object({ source: z.literal("import"), importSystem: z.string().min(1) }),
]);

export const CharacterSchema = z.object({
  id: z.string().min(1),
  roomId: z.string().min(1),
  system: z.string().min(1),
  name: z.string().min(1),
  level: z.number().int().min(1),
  class: z.string(),
  race: z.string(),
  stats: z.array(StatBlockSchema),
  skills: z.array(SkillSchema),
  origin: CharacterOriginSchema,
  version: z.number().int().min(0),
});

export const CharacterCreatePayloadSchema = CharacterSchema.omit({
  id: true,
  version: true,
});

export const CharacterUpdatePayloadSchema = CharacterSchema.partial().omit({
  id: true,
  roomId: true,
});

// ── TypeScript types (derived from schemas) ────────────────────────────────────

export type StatBlock = z.infer<typeof StatBlockSchema>;
export type Skill = z.infer<typeof SkillSchema>;
export type CharacterOrigin = z.infer<typeof CharacterOriginSchema>;
export type Character = z.infer<typeof CharacterSchema>;
export type CharacterCreatePayload = z.infer<
  typeof CharacterCreatePayloadSchema
>;
export type CharacterUpdatePayload = z.infer<
  typeof CharacterUpdatePayloadSchema
>;

// ── Supported systems enum (UI constraint) ────────────────────────────────────

export const SUPPORTED_SYSTEMS = [
  { value: "DND_5E", label: "D&D 5e" },
  //{ value: "generic", label: "Generic / Other" },
] as const;

export type SupportedSystem = (typeof SUPPORTED_SYSTEMS)[number]["value"];
