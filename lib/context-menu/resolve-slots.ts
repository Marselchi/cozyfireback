import {
  SEPARATOR,
  type ContextMenuPreset,
  type ContextMenuRuntimeContext,
  type ContextMenuItemDefinition,
} from "./types";
import { registry } from "./registry";

/**
 * Resolves a preset into the concrete list of item definitions that should
 * render for the given runtime context.
 *
 * Items whose `visible(ctx)` predicate returns false are dropped. Any
 * SEPARATOR that becomes orphaned as a result — leading, trailing, or
 * directly adjacent to another separator — is dropped too, so presets can
 * be written with "structural" separators without each one needing its own
 * visibility predicate.
 */
export function resolveVisibleSlots(
  preset: ContextMenuPreset,
  ctx: ContextMenuRuntimeContext,
): Array<ContextMenuItemDefinition | typeof SEPARATOR> {
  const withVisibility = preset.slots
    .map((slot) => {
      if (slot === SEPARATOR) return SEPARATOR;
      const def = registry[slot];
      if (!def) return null;
      if (def.visible && !def.visible(ctx)) return null;
      return def;
    })
    .filter(
      (slot): slot is ContextMenuItemDefinition | typeof SEPARATOR =>
        slot !== null,
    );

  // Trim leading/trailing separators and collapse consecutive ones.
  const result: Array<ContextMenuItemDefinition | typeof SEPARATOR> = [];
  for (const slot of withVisibility) {
    if (slot === SEPARATOR) {
      const previous = result.at(-1);
      if (result.length === 0 || previous === SEPARATOR) continue;
    }
    result.push(slot);
  }
  while (result.length > 0 && result.at(-1) === SEPARATOR) {
    result.pop();
  }


  return result;
}
