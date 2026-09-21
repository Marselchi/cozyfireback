import type React from "react";
import type { Editor } from "slate";
import type {
  EditorMode,
  CustomText,
  CustomElement,
  HeadingElement,
  SpoilerType,
  ChanceMetadata,
} from "@/types/editor";
import type { Role } from "@/types/editor-layout";

// ---------------------------------------------------------------------------
// Separator sentinel
// ---------------------------------------------------------------------------

export const SEPARATOR = "separator" as const;
export type Separator = typeof SEPARATOR;

// ---------------------------------------------------------------------------
// Item kind: mark
// ---------------------------------------------------------------------------

export type MarkItem = {
  kind: "mark";
  id: string;
  /** The CustomText key this mark toggles (e.g. "bold"). */
  format: keyof Omit<CustomText, "text">;
  icon: React.ComponentType<{ className?: string }>;
  label: string;
};

// ---------------------------------------------------------------------------
// Item kind: block
// ---------------------------------------------------------------------------

export type BlockItem = {
  kind: "block";
  id: string;
  /** The CustomElement type this block toggles (e.g. "blockquote"). */
  format: CustomElement["type"];
  icon: React.ComponentType<{ className?: string }>;
  label: string;
};

// ---------------------------------------------------------------------------
// Item kind: expandable (e.g. heading level picker)
// ---------------------------------------------------------------------------

/**
 * A single child inside an expandable item.
 * For headings, `format` is "heading" and `level` selects the heading level.
 * For other grouped items, `level` can be omitted.
 */
export type ExpandableChild = {
  id: string;
  label: string;
  format: CustomElement["type"];
  level?: HeadingElement["level"];
  icon?: React.ComponentType<{ className?: string }>;
};

export type ExpandableItem = {
  kind: "expandable";
  id: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  /**
   * The format used to determine whether the trigger button itself should
   * appear "active" (i.e. any of its children is currently active).
   */
  parentFormat: CustomElement["type"];
  children: ExpandableChild[];
};

// ---------------------------------------------------------------------------
// Item kind: custom (render-prop)
// ---------------------------------------------------------------------------

export type CustomItem = {
  kind: "custom";
  id: string;
  /**
   * Receives the full runtime context so the render-prop closure can access
   * the editor, mode, roles, and all other toolbar state.
   */
  render: (ctx: ToolbarRuntimeContext) => React.ReactNode;
};

// ---------------------------------------------------------------------------
// Discriminated union
// ---------------------------------------------------------------------------

export type ToolbarItemDefinition =
  | MarkItem
  | BlockItem
  | ExpandableItem
  | CustomItem;

// ---------------------------------------------------------------------------
// Preset
// ---------------------------------------------------------------------------

/** A slot in a preset is either an item id string or the SEPARATOR sentinel. */
export type PresetSlot = string | Separator;

export type ToolbarPreset = {
  id: string;
  slots: PresetSlot[];
};

// ---------------------------------------------------------------------------
// Chance config passed to the "chance" custom toolbar item
// ---------------------------------------------------------------------------

export type ToolbarChanceConfig = {
  id: string;
  spoilerType?: SpoilerType;
  chance?: ChanceMetadata;
  /**
   * Local Slate mutation only — never calls the network directly. The
   * caller (InlineEditorBlock) is responsible for applying this to the
   * element and marking the block dirty so it rides along with the next
   * real save, same as roles.
   */
  onChange: (patch: {
    spoilerType: SpoilerType;
    chance?: ChanceMetadata;
  }) => void;
};

// ---------------------------------------------------------------------------
// Runtime context — threaded through every item renderer and custom render-prop
// ---------------------------------------------------------------------------

export type ToolbarRuntimeContext = {
  editor: Editor;
  mode: EditorMode;
  onModeChange: (mode: EditorMode) => void;
  /** Whether the link button should be visible. Defaults to true. */
  showLinkButton: boolean;
  /** Main preset only: show/hide the "Спойлер" insert button. Defaults to true. */
  showInlineEditorButton?: boolean;
  /** Inline preset only: list of available roles for the role combobox. */
  roles?: Role[];
  /** Inline preset only: currently selected role ids. */
  selectedRoles?: string[];
  /** Inline preset only: callback to toggle a role by id. */
  onToggleRole?: (roleId: string) => void;
  /** Inline preset only: callback for the delete button. */
  onDelete?: () => void;
  /** Inline editor chance configuration — kept separate from the Slate
   * element props so the toolbar doesn't need to know Slate internals. */
  chanceConfig?: ToolbarChanceConfig;
  // --- link modal state (owned by Toolbar.tsx, threaded here for the custom item) ---
  onLinkClick: () => void;
};
