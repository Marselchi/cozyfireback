/**
 * Toolbar presets.
 *
 * Each preset is an ordered array of item ids (from the registry) and
 * SEPARATOR sentinels.  Adding a new variant means writing a new preset
 * object — no rendering code needs to change.
 *
 * Slot ids must match keys in the registry (lib/toolbar/registry.ts).
 */

import { SEPARATOR, type ToolbarPreset } from "./types";

// ---------------------------------------------------------------------------
// main
// Marks → blocks (with expandable heading) → link → mode toggle → spoiler.
// The "spoiler" custom item reads showInlineEditorButton from context and
// hides itself when it is false, so it is always listed here.
// ---------------------------------------------------------------------------

export const mainPreset: ToolbarPreset = {
  id: "main",
  slots: [
    "bold",
    "italic",
    "strikethrough",
    "code",
    SEPARATOR,
    "heading",
    "blockquote",
    "bulleted-list",
    "numbered-list",
    "code-block",
    SEPARATOR,
    "link",
    SEPARATOR,
    "mode-toggle",
    SEPARATOR,
    "spoiler",
  ],
};

// ---------------------------------------------------------------------------
// inline
// Drag handle → marks → blocks → link → mode toggle → roles → delete.
// The delete custom item uses ml-auto to push itself to the right edge.
// ---------------------------------------------------------------------------

export const inlinePreset: ToolbarPreset = {
  id: "inline",
  slots: [
    "drag-handle",
    SEPARATOR,
    "bold",
    "italic",
    "strikethrough",
    "code",
    SEPARATOR,
    "heading",
    "blockquote",
    "bulleted-list",
    "numbered-list",
    "code-block",
    SEPARATOR,
    "link",
    SEPARATOR,
    "mode-toggle",
    SEPARATOR,
    "roles",
    "chance",
    "delete",
  ],
};

// ---------------------------------------------------------------------------
// simple
// Identical to main minus spoiler, drag-handle, roles, and delete.
// ---------------------------------------------------------------------------

export const simplePreset: ToolbarPreset = {
  id: "simple",
  slots: [
    "bold",
    "italic",
    "strikethrough",
    "code",
    SEPARATOR,
    "heading",
    "blockquote",
    "bulleted-list",
    "numbered-list",
    "code-block",
    SEPARATOR,
    "link",
    SEPARATOR,
    "mode-toggle",
  ],
};
