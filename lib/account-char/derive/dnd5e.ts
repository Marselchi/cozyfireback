/**
 * D&D 5e modifier derivation rules.
 *
 * These functions are pure and stateless — no React, no fetch.
 * Add sibling files (e.g. pathfinder.ts) that export the same shape to
 * support additional systems.
 */

/**
 * Compute an ability score modifier from a raw ability score.
 * Standard formula: floor((score - 10) / 2)
 */
export function abilityModifier(score: number): number {
  return Math.floor((score - 10) / 2);
}

/**
 * Compute the proficiency bonus for a given character level.
 * Levels 1-4: +2, 5-8: +3, 9-12: +4, 13-16: +5, 17-20: +6
 */
export function proficiencyBonus(level: number): number {
  return Math.ceil(level / 4) + 1;
}

/**
 * Compute the total skill modifier.
 *
 * @param abilityScore  - The linked ability's raw score
 * @param proficiency   - 0 = none, 1 = proficient, 2 = expertise
 * @param level         - Character level (used to derive proficiency bonus)
 */
export function skillModifier(
  abilityScore: number,
  proficiency: number,
  level: number,
): number {
  const base = abilityModifier(abilityScore);
  const pb = proficiencyBonus(level);
  const multiplier = proficiency === 2 ? 2 : proficiency === 1 ? 1 : 0;
  return base + pb * multiplier;
}
