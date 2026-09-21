/**
 * Context menu presets.
 *
 * Each preset is an ordered array of item ids (from the registry) and
 * SEPARATOR sentinels. Separators that would end up orphaned once items are
 * filtered by their `visible(ctx)` predicate — leading, trailing, or
 * directly adjacent to another separator — are dropped by the renderer, so
 * a preset can be written with "structural" separators without needing a
 * `visible` predicate of its own. This reproduces the original component's
 * conditional `<ContextMenuSeparator />` placement without any JSX
 * conditionals living outside the registry.
 *
 * Slot ids must match keys in the registry (lib/context-menu/registry.ts).
 */

import { SEPARATOR, type ContextMenuPreset } from "./types";

// ---------------------------------------------------------------------------
// main
// Spoiler → link group → table group → generator group → template group.
// ---------------------------------------------------------------------------

export const mainPreset: ContextMenuPreset = {
  id: "main",
  slots: [
    "spoiler",
    "add-link",
    "change-url",
    "remove-link",
    "insert-table",
    SEPARATOR,
    "delete-row",
    "delete-column",
    "delete-table",
    SEPARATOR,
    "generate",
    "placeholder",
    SEPARATOR,
    "fill-placeholders",
    SEPARATOR,
    "save-template",
    "open-templates",
  ],
};
