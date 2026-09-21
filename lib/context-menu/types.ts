import type React from "react";
import type { Editor, Range } from "slate";
import type { EditorMode } from "@/types/editor";

// ---------------------------------------------------------------------------
// Separator sentinel (mirrors lib/toolbar/types.ts)
// ---------------------------------------------------------------------------

export const SEPARATOR = "separator" as const;
export type Separator = typeof SEPARATOR;

// ---------------------------------------------------------------------------
// Item kind: action
// A single clickable ContextMenuItem. `visible` / `disabled` are predicates
// over the runtime context so the registry stays declarative — no JSX
// conditionals live outside this file.
// ---------------------------------------------------------------------------

export type MenuAction = {
  kind: "action";
  id: string;
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  /** Defaults to always visible when omitted. */
  visible?: (ctx: ContextMenuRuntimeContext) => boolean;
  disabled?: (ctx: ContextMenuRuntimeContext) => boolean;
  variant?: "default" | "destructive";
  onSelect: (ctx: ContextMenuRuntimeContext) => void;
};

// ---------------------------------------------------------------------------
// Item kind: custom (render-prop, for menu entries that don't fit the
// standard icon+label+onSelect shape)
// ---------------------------------------------------------------------------

export type MenuCustom = {
  kind: "custom";
  id: string;
  visible?: (ctx: ContextMenuRuntimeContext) => boolean;
  render: (ctx: ContextMenuRuntimeContext) => React.ReactNode;
};

// ---------------------------------------------------------------------------
// Discriminated union
// ---------------------------------------------------------------------------

export type ContextMenuItemDefinition = MenuAction | MenuCustom;

// ---------------------------------------------------------------------------
// Preset
// ---------------------------------------------------------------------------

/** A slot in a preset is either an item id string or the SEPARATOR sentinel. */
export type MenuPresetSlot = string | Separator;

export type ContextMenuPreset = {
  id: string;
  slots: MenuPresetSlot[];
};

// ---------------------------------------------------------------------------
// Runtime context — threaded through every item's visible/disabled/onSelect
// ---------------------------------------------------------------------------

export type ContextMenuRuntimeContext = {
  editor: Editor;
  mode: EditorMode;
  savedSelection: Range | null;
  selectedText: string;

  // --- derived state, recomputed on every menu open ---
  isOnLink: boolean;
  isEditableLink: boolean;
  isInTableCell: boolean;
  canInsertInlineEditor: boolean;

  // --- feature flags (from props) ---
  showGeneratorOption: boolean;

  // --- action handlers (owned by the ContextMenu container) ---
  onAddInlineEditor: () => void;
  onAddLink: () => void;
  onChangeUrl: () => void;
  onRemoveLink: () => void;
  onInsertTable: () => void;
  onDeleteRow: () => void;
  onDeleteColumn: () => void;
  onDeleteTable: () => void;
  onOpenGenerator: () => void;
  onOpenPlaceholder: () => void;
  onFillPlaceholders: () => void;
  onSaveAsTemplate: () => void;
  onOpenTemplates: () => void;
};
