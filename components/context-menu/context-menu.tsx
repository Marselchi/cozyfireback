"use client";

import type React from "react";

import { useCallback, useRef, useState } from "react";
import {
  Editor,
  Range,
  Element as SlateElement,
  Transforms,
  Node,
} from "slate";
import { HistoryEditor } from "slate-history";
import { useSlate, ReactEditor } from "slate-react";
import {
  ContextMenu as ContextMenuRoot,
  ContextMenuContent,
  ContextMenuSeparator,
  ContextMenuTrigger,
} from "@/components/ui/context-menu";
import {
  insertInlineEditor,
  insertLink,
  unwrapLink,
  getLinkAtSelection,
  getTableCellAtSelection,
  deleteTable,
  deleteTableColumn,
  deleteTableRow,
  insertTable,
} from "@/lib/editor-utils";
import { LinkModal, type LinkModalData } from "@/components/editor/link-modal";
import type { EditorMode, LinkElement } from "@/types/editor";
import { toast } from "sonner";
import { useGeneratorData } from "@/hooks/use-generator-data";
import { PlaceholderDialog } from "@/components/editor/placeholder-dialog";
import { AutoGeneratorDialog } from "@/components/editor/auto-generator-dialog";
import { SaveTemplateDialog } from "@/components/editor/save-template-dialog";
import { TemplateModal } from "@/components/editor/template-modal";

import {
  SEPARATOR,
  type ContextMenuPreset,
  type ContextMenuRuntimeContext,
  type ContextMenuItemDefinition,
} from "@/lib/context-menu/types";
import { resolveVisibleSlots } from "@/lib/context-menu/resolve-slots";
import { ActionItem } from "./action-item";
import { CustomItem } from "./custom-item";

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

export type EditorContextMenuProps = {
  children: React.ReactNode;
  preset: ContextMenuPreset;
  showInlineEditorOption?: boolean;
  isInsideInlineEditor?: boolean;
  showGeneratorOption?: boolean;
  mode: EditorMode;
};

/**
 * Regex that matches {{ token }} placeholders.
 * token format: category.subType.modifier or category.subType.modifier.attribute
 * e.g. name.first.elf or name.first.elf.male
 */
const PLACEHOLDER_REGEX = /\{\{([a-z]+\.[a-z]+\.[a-z]+(?:\.[a-z]+)?)\}\}/gi;

/**
 * Snapshot of editor-derived state needed to build the context menu's
 * runtime context. Computed synchronously (no setState) so it can be
 * produced and consumed in a single pass when the menu opens.
 */
type ContextSnapshot = {
  savedSelection: Range | null;
  isOnLink: boolean;
  isEditableLink: boolean;
  selectedText: string;
  isInTableCell: boolean;
};

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function EditorContextMenu({
  children,
  preset,
  showInlineEditorOption = true,
  isInsideInlineEditor = false,
  showGeneratorOption = true,
  mode = "parsed",
}: Readonly<EditorContextMenuProps>) {
  const editor = useSlate();
  const { ensureData, sampleToken } = useGeneratorData();

  // ─── Link / Spoiler state ──────────────────────────────────────────────────
  const [linkModalOpen, setLinkModalOpen] = useState(false);
  const [linkModalData, setLinkModalData] = useState<
    LinkModalData | undefined
  >();

  // savedSelectionRef is the single source of truth for *reading* the saved
  // selection inside callbacks. Refs update synchronously, so a handler
  // fired right after a menu click always sees the latest value — unlike
  // the `savedSelection` state below, whose update may not have flushed
  // yet by the time a click handler runs (stale-closure risk).
  const savedSelectionRef = useRef<Range | null>(null);

  // savedSelection (state) is kept only to drive re-renders / conditional
  // UI. Any handler that needs to *restore* a selection should read
  // savedSelectionRef.current instead of this.
  const [selectedText, setSelectedText] = useState("");
  const [savedSelection, setSavedSelection] = useState<Range | null>(null);

  // ─── Generator state ───────────────────────────────────────────────────────
  const [generatorOpen, setGeneratorOpen] = useState(false);
  const [placeholderOpen, setPlaceholderOpen] = useState(false);

  // ─── Template state ────────────────────────────────────────────────────────
  const [saveTemplateOpen, setSaveTemplateOpen] = useState(false);
  const [templateModalOpen, setTemplateModalOpen] = useState(false);

  // ─── Table state ────────────────────────────────────────────────────────
  const [isInTableCell, setIsInTableCell] = useState(false);

  // ─── Resolved menu state (only recomputed when the menu opens) ────────────
  const [visibleSlots, setVisibleSlots] = useState<
    Array<ContextMenuItemDefinition | typeof SEPARATOR>
  >([]);
  const [runtimeCtx, setRuntimeCtx] =
    useState<ContextMenuRuntimeContext | null>(null);

  /** Keep the ref and the state in sync in one place. */
  const setSavedSelectionBoth = useCallback((selection: Range | null) => {
    savedSelectionRef.current = selection;
    setSavedSelection(selection);
  }, []);

  // ─── Context snapshot (pure, synchronous — no setState) ───────────────────
  const computeContextSnapshot = useCallback((): ContextSnapshot => {
    const { selection } = editor;
    if (!selection) {
      return {
        savedSelection: null,
        isOnLink: false,
        isEditableLink: false,
        selectedText: "",
        isInTableCell: false,
      };
    }

    let isOnLink = false;
    let isEditableLink = false;

    const linkEntry = getLinkAtSelection(editor);
    if (linkEntry) {
      const [linkNode] = linkEntry;
      const link = linkNode as LinkElement;
      isOnLink = true;

      try {
        const url = new URL(link.url);
        const domain = process.env.NEXT_PUBLIC_DOMAIN ?? "";

        const urlHost = url.host;
        isEditableLink =
          urlHost === domain ||
          urlHost.endsWith(`.${domain}`) ||
          domain.includes(urlHost);
      } catch {
        isEditableLink = false;
      }
    }

    const isInTableCell = !!getTableCellAtSelection(editor);
    const selectedText = !Range.isCollapsed(selection)
      ? Editor.string(editor, selection)
      : "";

    // Clone the selection (including nested anchor/focus points) so this
    // snapshot can't be affected by Slate later transforming the live
    // editor.selection object out from under us.
    const clonedSelection: Range = {
      ...selection,
      anchor: { ...selection.anchor },
      focus: { ...selection.focus },
    };

    return {
      savedSelection: clonedSelection,
      isOnLink,
      isEditableLink,
      selectedText,
      isInTableCell,
    };
  }, [editor]);

  const checkContextState = useCallback((): ContextSnapshot => {
    const snapshot = computeContextSnapshot();
    setSavedSelectionBoth(snapshot.savedSelection);
    setSelectedText(snapshot.selectedText);
    setIsInTableCell(snapshot.isInTableCell);
    return snapshot;
  }, [computeContextSnapshot, setSavedSelectionBoth]);

  // ─── Spoiler ───────────────────────────────────────────────────────────────
  const handleAddInlineEditor = useCallback(() => {
    insertInlineEditor(editor);
    setTimeout(() => ReactEditor.focus(editor), 0);
  }, [editor]);

  const canInsertInlineEditor = useCallback(() => {
    if (isInsideInlineEditor) return false;
    if (!showInlineEditorOption) return false;
    if (isInTableCell) return false;
    const { selection } = editor;
    if (!selection) return false;
    const [inlineEditor] = Editor.nodes(editor, {
      match: (n) => SlateElement.isElement(n) && n.type === "inline-editor",
    });
    return !inlineEditor;
  }, [editor, showInlineEditorOption, isInsideInlineEditor, isInTableCell]);

  // ─── Link handlers ─────────────────────────────────────────────────────────
  const handleAddLink = useCallback(() => {
    setLinkModalData({ name: selectedText, url: "", isEditing: false });
    setLinkModalOpen(true);
  }, [selectedText]);

  const handleChangeUrl = useCallback(() => {
    const linkEntry = getLinkAtSelection(editor);
    if (linkEntry) {
      const [linkNode] = linkEntry;
      const link = linkNode as LinkElement;
      const linkText = Node.string(linkNode);
      setLinkModalData({ name: linkText, url: link.url, isEditing: true });
      setLinkModalOpen(true);
    }
  }, [editor]);

  const handleRemoveLink = useCallback(() => {
    const selection = savedSelectionRef.current;
    if (!selection) return;

    try {
      Transforms.unwrapNodes(editor, {
        at: selection, // explicit target instead of relying on ambient editor.selection
        split: true,
        match: (n) =>
          !Editor.isEditor(n) && SlateElement.isElement(n) && n.type === "link",
      });
    } catch (err) {
      console.error("Failed to remove link:", err);
      return;
    }

    setTimeout(() => {
      try {
        ReactEditor.focus(editor);
      } catch {}
    }, 0);
  }, [editor]);

  const handleLinkSubmit = useCallback(
    (data: LinkModalData) => {
      const selection = savedSelectionRef.current;
      if (selection) {
        try {
          Transforms.select(editor, selection);
        } catch {}
      }

      if (data.isEditing) {
        const linkEntry = getLinkAtSelection(editor);
        if (linkEntry) {
          const [, path] = linkEntry;
          Transforms.setNodes(
            editor,
            { url: data.url } as Partial<LinkElement>,
            { at: path },
          );
          const [linkNode] = linkEntry;
          const currentText = Node.string(linkNode);
          if (currentText !== data.name) {
            Transforms.select(editor, path);
            unwrapLink(editor);
            insertLink(editor, data.url, data.name, undefined, mode);
          }
        }
      } else {
        insertLink(
          editor,
          data.url,
          data.name || data.url,
          selectedText.length > 0,
          mode,
        );
      }

      setLinkModalOpen(false);
      setTimeout(() => {
        try {
          ReactEditor.focus(editor);
          const { selection } = editor;
          if (selection) Transforms.move(editor, { unit: "offset" });
        } catch (error) {
          console.error(error);
        }
      }, 100);
    },
    [editor, selectedText, mode],
  );

  const handleModalOpenChange = useCallback((open: boolean) => {
    if (!open) setLinkModalOpen(false);
  }, []);

  // ─── Table handlers ─────────────────────────────────────────────────────
  const handleDeleteRow = useCallback(() => {
    const selection = savedSelectionRef.current;
    if (selection) {
      try {
        Transforms.select(editor, selection);
      } catch {}
    }
    const cellEntry = getTableCellAtSelection(editor);
    if (!cellEntry) return;
    const [, cellPath] = cellEntry;
    const tablePath = cellPath.slice(0, -2);
    const rowIndex = cellPath[cellPath.length - 2];
    deleteTableRow(editor, tablePath, rowIndex);
    setTimeout(() => {
      try {
        ReactEditor.focus(editor);
      } catch {}
    }, 0);
  }, [editor]);

  const handleDeleteColumn = useCallback(() => {
    const selection = savedSelectionRef.current;
    if (selection) {
      try {
        Transforms.select(editor, selection);
      } catch {}
    }
    const cellEntry = getTableCellAtSelection(editor);
    if (!cellEntry) return;
    const [, cellPath] = cellEntry;
    const tablePath = cellPath.slice(0, -2);
    const colIndex = cellPath[cellPath.length - 1];
    deleteTableColumn(editor, tablePath, colIndex);
    setTimeout(() => {
      try {
        ReactEditor.focus(editor);
      } catch {}
    }, 0);
  }, [editor]);

  const handleDeleteTable = useCallback(() => {
    const selection = savedSelectionRef.current;
    if (selection) {
      try {
        Transforms.select(editor, selection);
      } catch {}
    }
    const cellEntry = getTableCellAtSelection(editor);
    if (!cellEntry) return;
    const [, cellPath] = cellEntry;
    const tablePath = cellPath.slice(0, -2);
    deleteTable(editor, tablePath);
    setTimeout(() => {
      try {
        ReactEditor.focus(editor);
      } catch {}
    }, 0);
  }, [editor]);

  const handleInsertTable = useCallback(() => {
    insertTable(editor);
  }, [editor]);

  // ─── Generator helpers ─────────────────────────────────────────────────────

  /** Insert arbitrary text at the saved (or current) selection */
  const insertTextAtSaved = useCallback(
    (text: string) => {
      const selection = savedSelectionRef.current;
      try {
        if (selection) Transforms.select(editor, selection);
      } catch {
        // selection may be stale — fall through to current position
      }
      Transforms.insertText(editor, text);
      setTimeout(() => {
        try {
          ReactEditor.focus(editor);
        } catch {}
      }, 0);
    },
    [editor],
  );

  const handleOpenGenerator = useCallback(() => {
    setSavedSelectionBoth(editor.selection ? { ...editor.selection } : null);
    setGeneratorOpen(true);
  }, [editor, setSavedSelectionBoth]);

  const handleOpenPlaceholder = useCallback(() => {
    setSavedSelectionBoth(editor.selection ? { ...editor.selection } : null);
    setPlaceholderOpen(true);
  }, [editor, setSavedSelectionBoth]);

  // ─── Fill Placeholders ─────────────────────────────────────────────────────
  const handleFillPlaceholders = useCallback(async () => {
    const generatorData = await ensureData();
    if (!generatorData) {
      toast.error("Ошибка генератора");
      return;
    }

    let filledCount = 0;

    Editor.withoutNormalizing(editor, () => {
      const entries: Array<{ path: number[]; text: string }> = [];
      for (const [node, path] of Editor.nodes(editor, {
        at: [],
        match: (n) => !Editor.isEditor(n) && "text" in n,
      })) {
        const textNode = node as { text: string };
        if (PLACEHOLDER_REGEX.test(textNode.text)) {
          entries.push({ path: path as number[], text: textNode.text });
        }
        PLACEHOLDER_REGEX.lastIndex = 0;
      }

      for (let i = entries.length - 1; i >= 0; i--) {
        const { path, text } = entries[i];
        const modified = text.replace(
          PLACEHOLDER_REGEX,
          (match, token: string) => {
            const parts = token.split(".");
            const [cat, sub, mod, attr] = parts;
            const value = sampleToken(generatorData, cat, sub, mod, attr);
            if (value) {
              filledCount++;
              return value;
            }
            return match;
          },
        );
        PLACEHOLDER_REGEX.lastIndex = 0;

        if (modified !== text) {
          try {
            const anchor = { path, offset: 0 };
            const focus = { path, offset: text.length };
            Transforms.insertText(editor, modified, { at: { anchor, focus } });
          } catch {
            // ignore stale paths
          }
        }
      }
    });

    if (filledCount > 0) {
      toast.success(`Заполнено ${filledCount} плейсхолдеров`);
    } else {
      toast.info("В документе нет плейсхолдеров");
    }

    try {
      ReactEditor.focus(editor);
    } catch {}
  }, [editor, ensureData, sampleToken]);

  // ─── Template handlers ─────────────────────────────────────────────────────

  const handleSaveAsTemplate = useCallback(() => {
    setSaveTemplateOpen(true);
  }, []);

  const handleOpenTemplates = useCallback(() => {
    setSavedSelectionBoth(editor.selection ? { ...editor.selection } : null);
    setTemplateModalOpen(true);
  }, [editor, setSavedSelectionBoth]);

  /** Вставляет шаблон в позицию курсора */
  const handleApplyTemplate = useCallback(
    (content: import("slate").Descendant[]) => {
      // Восстанавливаем сохранённую селекцию
      const selection = savedSelectionRef.current;
      if (selection) {
        try {
          Transforms.select(editor, selection);
        } catch {
          // Селекция может быть устаревшей — используем текущую
        }
      }

      // Удаляем выделение если есть
      if (editor.selection && !Range.isCollapsed(editor.selection)) {
        Transforms.delete(editor);
      }

      Editor.withoutNormalizing(editor, () => {
        HistoryEditor.withoutSaving(editor, () => {
          Transforms.insertFragment(editor, content);
        });
      });

      editor.onChange();

      setTimeout(() => {
        try {
          ReactEditor.focus(editor);
        } catch {}
      }, 0);
    },
    [editor],
  );

  // ─── Runtime context threaded through every registry item ─────────────────
  const buildRuntimeContext = useCallback(
    (snapshot: ContextSnapshot): ContextMenuRuntimeContext => ({
      editor,
      mode,
      savedSelection: snapshot.savedSelection,
      selectedText: snapshot.selectedText,
      isOnLink: snapshot.isOnLink,
      isEditableLink: snapshot.isEditableLink,
      isInTableCell: snapshot.isInTableCell,
      canInsertInlineEditor: canInsertInlineEditor(),
      showGeneratorOption,
      onAddInlineEditor: handleAddInlineEditor,
      onAddLink: handleAddLink,
      onChangeUrl: handleChangeUrl,
      onRemoveLink: handleRemoveLink,
      onInsertTable: handleInsertTable,
      onDeleteRow: handleDeleteRow,
      onDeleteColumn: handleDeleteColumn,
      onDeleteTable: handleDeleteTable,
      onOpenGenerator: handleOpenGenerator,
      onOpenPlaceholder: handleOpenPlaceholder,
      onFillPlaceholders: handleFillPlaceholders,
      onSaveAsTemplate: handleSaveAsTemplate,
      onOpenTemplates: handleOpenTemplates,
    }),
    [
      editor,
      mode,
      canInsertInlineEditor,
      showGeneratorOption,
      handleAddInlineEditor,
      handleAddLink,
      handleChangeUrl,
      handleRemoveLink,
      handleInsertTable,
      handleDeleteRow,
      handleDeleteColumn,
      handleDeleteTable,
      handleOpenGenerator,
      handleOpenPlaceholder,
      handleFillPlaceholders,
      handleSaveAsTemplate,
      handleOpenTemplates,
    ],
  );

  // ─── Menu open handler — the only place resolveVisibleSlots runs ──────────
  const handleContextMenuOpenChange = useCallback(
    (open: boolean) => {
      if (!open) return;

      const snapshot = checkContextState();
      const nextRuntimeCtx = buildRuntimeContext(snapshot);

      setRuntimeCtx(nextRuntimeCtx);
      setVisibleSlots(resolveVisibleSlots(preset, nextRuntimeCtx));
    },
    [checkContextState, buildRuntimeContext, preset],
  );

  return (
    <>
      <ContextMenuRoot onOpenChange={handleContextMenuOpenChange}>
        <ContextMenuTrigger asChild>{children}</ContextMenuTrigger>

        <ContextMenuContent className="w-52">
          {runtimeCtx &&
            visibleSlots.map((slot, i) => {
              if (slot === SEPARATOR) {
                return <ContextMenuSeparator key={`sep-${i}`} />;
              }

              switch (slot.kind) {
                case "action":
                  return (
                    <ActionItem key={slot.id} item={slot} ctx={runtimeCtx} />
                  );
                case "custom":
                  return (
                    <CustomItem key={slot.id} item={slot} ctx={runtimeCtx} />
                  );
              }
            })}
        </ContextMenuContent>
      </ContextMenuRoot>

      <LinkModal
        open={linkModalOpen}
        onOpenChange={handleModalOpenChange}
        initialData={linkModalData}
        onSubmit={handleLinkSubmit}
      />

      <AutoGeneratorDialog
        open={generatorOpen}
        onOpenChange={setGeneratorOpen}
        onInsert={insertTextAtSaved}
      />

      <PlaceholderDialog
        open={placeholderOpen}
        onOpenChange={setPlaceholderOpen}
        onInsert={insertTextAtSaved}
      />

      <SaveTemplateDialog
        open={saveTemplateOpen}
        onOpenChange={setSaveTemplateOpen}
        content={editor.children}
      />

      <TemplateModal
        open={templateModalOpen}
        onOpenChange={setTemplateModalOpen}
        onApply={handleApplyTemplate}
      />
    </>
  );
}
