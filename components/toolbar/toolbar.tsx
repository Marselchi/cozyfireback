"use client";

import { useState, useCallback } from "react";
import { useSlate } from "slate-react";
import { Editor, Range } from "slate";

import { Separator } from "@/components/ui/separator";

import { registry } from "@/lib/toolbar/registry";
import {
  SEPARATOR,
  type ToolbarPreset,
  type ToolbarRuntimeContext,
  type ToolbarChanceConfig,
} from "@/lib/toolbar/types";
import type { EditorMode } from "@/types/editor";
import type { Role } from "@/types/editor-layout";

import { insertLink } from "@/lib/editor-utils";

import { MarkItem } from "./mark-item";
import { BlockItem } from "./block-item";
import { ExpandableItem } from "./expandable-item";
import { CustomItem } from "./custom-item";
import { LinkModal, LinkModalData } from "../editor/link-modal";

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

export type ToolbarProps = {
  preset: ToolbarPreset;
  mode: EditorMode;
  onModeChange: (mode: EditorMode) => void;
  showLinkButton?: boolean;
  // main preset
  showInlineEditorButton?: boolean;
  // inline preset
  roles?: Role[];
  selectedRoles?: string[];
  onToggleRole?: (roleId: string) => void;
  onDelete?: () => void;
  chanceConfig?: ToolbarChanceConfig;
};

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function Toolbar({
  preset,
  mode,
  onModeChange,
  showLinkButton = true,
  showInlineEditorButton,
  roles,
  selectedRoles,
  onToggleRole,
  onDelete,
  chanceConfig,
}: Readonly<ToolbarProps>) {
  const editor = useSlate();

  const [linkModalOpen, setLinkModalOpen] = useState(false);
  const [selectedText, setSelectedText] = useState("");

  const stopMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleLinkClick = useCallback(() => {
    const { selection } = editor;
    if (selection && !Range.isCollapsed(selection)) {
      setSelectedText(Editor.string(editor, selection));
    } else {
      setSelectedText("");
    }
    setLinkModalOpen(true);
  }, [editor]);

  const handleLinkSubmit = useCallback(
    (data: LinkModalData) => {
      insertLink(
        editor,
        data.url,
        data.name || data.url,
        selectedText.length > 0,
        mode,
      );
    },
    [editor, selectedText, mode],
  );

  // Build the runtime context that is threaded through every item renderer.
  const ctx: ToolbarRuntimeContext = {
    editor,
    mode,
    onModeChange,
    showLinkButton,
    showInlineEditorButton,
    roles,
    selectedRoles,
    onToggleRole,
    onDelete,
    chanceConfig,
    onLinkClick: handleLinkClick,
  };

  return (
    <>
      <div
        className="flex flex-wrap items-center gap-1 border border-b-0 border-border bg-muted p-1.5 font-sans"
        contentEditable={false}
        suppressContentEditableWarning
        onMouseDown={stopMouseDown}
      >
        {preset.slots.map((slot, i) => {
          if (slot === SEPARATOR) {
            return (
              <Separator
                key={`sep-${i}`}
                orientation="vertical"
                className="mx-1 h-5 bg-input justify-self-center"
              />
            );
          }

          const def = registry[slot];
          if (!def) return null;

          switch (def.kind) {
            case "mark":
              return <MarkItem key={def.id} item={def} ctx={ctx} />;
            case "block":
              return <BlockItem key={def.id} item={def} ctx={ctx} />;
            case "expandable":
              return <ExpandableItem key={def.id} item={def} ctx={ctx} />;
            case "custom":
              return <CustomItem key={def.id} item={def} ctx={ctx} />;
          }
        })}
      </div>

      <LinkModal
        open={linkModalOpen}
        onOpenChange={setLinkModalOpen}
        initialData={{ name: selectedText, url: "", isEditing: false }}
        onSubmit={handleLinkSubmit}
      />
    </>
  );
}
