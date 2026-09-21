/**
 * Generator data types.
 * Scalable: add new top-level categories (e.g. "creature", "item") and new
 * sub-types / modifiers at will — the dialogs pick them up automatically.
 */

/** A leaf pool holds the actual string values that can be sampled */
export interface GeneratorPool {
  label: string
  values: string[]
}

/** A sub-type groups pools together (e.g. first vs last name) */
export interface GeneratorSubType {
  label: string
  /** keyed by modifier such as "elf", "human", "dwarf" */
  pools: Record<string, GeneratorModifier>
}

/** A modifier groups attribute pools together (e.g. male, female, neutral) */
export interface GeneratorModifierWithAttributes {
  label: string
  /** keyed by attribute such as "male", "female", "neutral" */
  attributes: Record<string, GeneratorPool>
}

/** A modifier with direct values (no attributes) */
export interface GeneratorModifierWithValues {
  label: string
  /** Direct values for modifiers without attributes */
  values: string[]
}

/** A modifier can have either attributes or direct values */
export type GeneratorModifier = GeneratorModifierWithAttributes | GeneratorModifierWithValues

/** A top-level category (person, place, organisation …) */
export interface GeneratorCategory {
  label: string
  /** keyed by sub-type such as "first", "last", "full" */
  subTypes: Record<string, GeneratorSubType>
}

/** The entire data set that comes from the server / is stored in localStorage */
export type GeneratorData = Record<string, GeneratorCategory>

/**
 * Placeholder spec encoded in the editor text.
 * e.g. {{ category.subType.modifier }} → name.first.elf
 * or   {{ category.subType.modifier.attribute }} → name.first.elf.male
 */
export interface PlaceholderSpec {
  category: string
  subType: string
  modifier: string
  /** Optional fourth level — e.g. "male", "female" */
  attribute?: string
  /** Original raw token text, e.g. "name.first.elf.male" */
  token: string
}
