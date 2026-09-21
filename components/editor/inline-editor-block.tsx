"use client";

import type React from "react";
import { useState, useMemo, useCallback, useRef, useEffect } from "react";
import {
  createEditor,
  type Descendant,
  type NodeEntry,
  Transforms,
  Editor,
} from "slate";
import { Slate, Editable, withReact, useSlateStatic } from "slate-react";
import { withHistory, HistoryEditor } from "slate-history";
import { Badge } from "@/components/ui/badge";
import { X } from "lucide-react";
import type {
  EditorMode,
  InlineEditorElement,
  SpoilerType,
  ChanceMetadata,
} from "@/types/editor";
import {
  removeInlineEditor,
  updateInlineEditorContent,
  updateInlineEditorRoles,
  updateInlineEditorSpoiler,
  withShortcuts,
  withInlineEditors,
  parseMarkdownToSlate,
  insertLink,
  withBlockEnter,
  withTables,
} from "@/lib/editor-utils";
import { ElementRenderer } from "./element-renderer";
import { LeafRenderer } from "./leaf-renderer";
import { EditorContextMenu } from "../context-menu";
import { LinkModal, type LinkModalData } from "./link-modal";
import { cn } from "@/lib/utils";
import { Role } from "@/types/editor-layout";
import {
  convertParsedToRaw,
  convertRawToParsed,
  decorateMarkdown,
} from "@/lib/editor";
import { Toolbar } from "../toolbar";
import { inlinePreset } from "@/lib/toolbar/presets";
import { mainPreset } from "@/lib/context-menu/presets";
import { useEditorState } from "./editor-state-provider";

type InlineEditorBlockProps = {
  element: InlineEditorElement;
  mode: EditorMode;
  roles: Role[];
  children: React.ReactNode;
};

const EMPTY_CONTENT: Descendant[] = [
  { type: "paragraph" as const, children: [{ text: "" }] },
];

// Border/background per spoiler type — this is the "block styles" signal
// requested: a chance spoiler should visually read as different from a
// plain one at a glance, not just via the toolbar dropdown.
const SPOILER_STYLE: Record<SpoilerType, string> = {
  normal: "border-amber-400 bg-foreground/20",
  chance: "border-violet-400 bg-violet-500/10",
};

export function InlineEditorBlock({
  element,
  mode: parentMode,
  roles,
  children,
}: Readonly<InlineEditorBlockProps>) {
  const parentEditor = useSlateStatic();
  const [inlineMode, setInlineMode] = useState<EditorMode>(parentMode);
  const [selectedRoles, setSelectedRoles] = useState<string[]>(
    element.roles || [],
  );
  const markInlineBlockDirty = useEditorState((s) => s.markInlineBlockDirty);
  const markInlineBlockDeleted = useEditorState(
    (s) => s.markInlineBlockDeleted,
  );
  const [linkModalOpen, setLinkModalOpen] = useState(false);

  const modeRef = useRef<EditorMode>(parentMode);
  modeRef.current = inlineMode;

  // Мемоизируем начальный контент
  const initialParsedContent = useMemo(
    () =>
      element.content && element.content.length > 0
        ? element.content
        : EMPTY_CONTENT,
    [element.content],
  );

  const initialRawContent = useMemo(
    () => convertParsedToRaw(initialParsedContent),
    [initialParsedContent],
  );

  const inlineEditor = useMemo(() => {
    const e = withHistory(withReact(createEditor()));
    return withBlockEnter(
      withTables(
        withShortcuts(withInlineEditors(e), () => modeRef.current === "parsed"),
      ),
    );
  }, []);

  // editor.children is the single source of truth for content once mounted —
  // no parallel React state mirroring it. `initialValue` below is read by
  // Slate exactly once, at mount; which of the two pre-converted forms it
  // gets just depends on which mode we're starting in.
  const initialValue = useMemo(
    () => (parentMode === "raw" ? initialRawContent : initialParsedContent),
    [parentMode, initialRawContent, initialParsedContent],
  );

  const updateTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (updateTimerRef.current) clearTimeout(updateTimerRef.current);
    };
  }, []);

  const handleDelete = useCallback(() => {
    markInlineBlockDeleted(element.realId);
    removeInlineEditor(parentEditor, element.id);
  }, [parentEditor, element.id, element.realId, markInlineBlockDeleted]);

  const handleRoleToggle = useCallback(
    (roleId: string) => {
      const newRoles = selectedRoles.includes(roleId)
        ? selectedRoles.filter((r) => r !== roleId)
        : [...selectedRoles, roleId];
      setSelectedRoles(newRoles);
      updateInlineEditorRoles(parentEditor, element.id, newRoles);
      markInlineBlockDirty(element.id);
    },
    [parentEditor, element.id, selectedRoles],
  );

  // Chance config edits, same shape as roles above: write the value onto
  // the Slate node locally and flag the block dirty. Nothing here talks
  // to the server directly — whether that ends up as an addedBlocks or
  // updatedBlocks entry on the next save is decided by whether the block
  // already has a realId (see getInlineBlocksDiff), same as any other
  // metadata change. A freshly-inserted spoiler has no realId yet, so a
  // direct PATCH-on-change (the old behavior) was a silent no-op for it.
  const handleChanceChange = useCallback(
    (patch: { spoilerType: SpoilerType; chance?: ChanceMetadata }) => {
      updateInlineEditorSpoiler(parentEditor, element.id, patch);
      markInlineBlockDirty(element.id);
    },
    [parentEditor, element.id, markInlineBlockDirty],
  );

  const handleModeChange = useCallback(
    (newMode: EditorMode) => {
      if (newMode === inlineMode) return;

      HistoryEditor.withoutSaving(inlineEditor, () => {
        Transforms.deselect(inlineEditor);
      });
      inlineEditor.history = { undos: [], redos: [] };

      const currentContent = inlineEditor.children as Descendant[];
      const converted =
        newMode === "raw"
          ? convertParsedToRaw(currentContent)
          : convertRawToParsed(currentContent);
      const nextValue = converted.length > 0 ? converted : EMPTY_CONTENT;

      inlineEditor.children = nextValue;
      Transforms.select(inlineEditor, Editor.start(inlineEditor, []));
      inlineEditor.onChange();

      // The parent always wants the parsed form regardless of which
      // direction we're switching: going parsed -> raw, the content itself
      // (currentContent) is still the parsed form we had a moment ago;
      // going raw -> parsed, nextValue IS the freshly parsed form.
      const parsedForParent = newMode === "raw" ? currentContent : nextValue;
      updateInlineEditorContent(parentEditor, element.id, parsedForParent);
      markInlineBlockDirty(element.id);
      setInlineMode(newMode);
    },
    [inlineMode, inlineEditor, parentEditor, element.id, markInlineBlockDirty],
  );

  // Оптимизированный debounced обработчик
  const handleValueChange = useCallback(
    (value: Descendant[]) => {
      if (updateTimerRef.current) clearTimeout(updateTimerRef.current);

      if (inlineMode === "raw") {
        updateTimerRef.current = setTimeout(() => {
          try {
            const markdown = value
              .map((node) => {
                if ("children" in node) {
                  return (node.children as any[])
                    .map((c) => c.text || "")
                    .join("");
                }
                return "";
              })
              .join("\n");
            const parsedNodes = parseMarkdownToSlate(markdown);
            updateInlineEditorContent(parentEditor, element.id, parsedNodes);
            markInlineBlockDirty(element.id);
          } catch (error) {
            console.error(error);
          }
        }, 300);
      } else {
        updateTimerRef.current = setTimeout(() => {
          updateInlineEditorContent(parentEditor, element.id, value);
        }, 300);
        markInlineBlockDirty(element.id);
      }
    },
    [parentEditor, element.id, inlineMode, markInlineBlockDirty],
  );

  const renderElement = useCallback(
    (props: any) => (
      <ElementRenderer {...props} mode={inlineMode} roles={roles} />
    ),
    [inlineMode, roles],
  );

  const renderLeaf = useCallback(
    (props: any) => <LeafRenderer {...props} />,
    [inlineMode],
  );

  const decorate = useCallback(
    (entry: NodeEntry) => {
      if (inlineMode === "raw") {
        return decorateMarkdown(entry);
      }
      return [];
    },
    [inlineMode],
  );

  const handleLinkSubmit = useCallback(
    (data: LinkModalData) => {
      insertLink(
        inlineEditor,
        data.url,
        data.name || data.url,
        false,
        modeRef.current,
      );
    },
    [inlineEditor],
  );

  const spoilerType = element.spoilerType ?? "normal";
  const hasRoleOrChanceBadges =
    selectedRoles.length > 0 || (spoilerType === "chance" && element.chance);

  return (
    <div
      className={cn(
        "my-3 rounded-lg border-2 select-none font-sans",
        SPOILER_STYLE[spoilerType],
      )}
      onMouseDown={(e) => e.stopPropagation()}
      onClick={(e) => e.stopPropagation()}
      onKeyDown={(e) => e.stopPropagation()}
    >
      {hasRoleOrChanceBadges && (
        <div className="flex items-center gap-2 flex-wrap p-2">
          {spoilerType === "chance" && element.chance && (
            <Badge
              variant="outline"
              className="text-xs py-0 h-5 gap-1 border-violet-400 text-violet-600 dark:text-violet-400"
            >
              Шанс: {element.chance.skill} ≥ {element.chance.threshold}
            </Badge>
          )}
          {selectedRoles.map((roleId) => {
            const role = roles.find((r) => r.id === Number.parseInt(roleId));
            return role ? (
              <Badge
                key={roleId}
                variant="secondary"
                className="text-xs py-0 h-5 gap-1"
              >
                {role.name}
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    handleRoleToggle(roleId);
                  }}
                  className="hover:bg-muted rounded-full"
                >
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            ) : null;
          })}
        </div>
      )}
      <div className="select-text">
        <Slate
          editor={inlineEditor}
          initialValue={initialValue}
          onValueChange={handleValueChange}
        >
          <Toolbar
            preset={inlinePreset}
            mode={inlineMode}
            onModeChange={handleModeChange}
            showLinkButton={true}
            roles={roles}
            selectedRoles={selectedRoles}
            onToggleRole={handleRoleToggle}
            onDelete={handleDelete}
            chanceConfig={{
              id: element.id ?? element.realId ?? "inline-editor",
              spoilerType: element.spoilerType,
              chance: element.chance,
              onChange: handleChanceChange,
            }}
          />
          <EditorContextMenu
            preset={mainPreset}
            showInlineEditorOption={false}
            isInsideInlineEditor={true}
            mode={inlineMode}
          >
            <div
              onMouseDown={(e) => e.stopPropagation()}
              onClick={(e) => e.stopPropagation()}
              onKeyDown={(e) => e.stopPropagation()}
            >
              <Editable
                className={cn(
                  "min-h-20 rounded-b-md border border-t-0 border-border bg-background p-3 focus:outline-none",
                )}
                renderElement={renderElement}
                renderLeaf={renderLeaf}
                decorate={decorate}
                placeholder="Пишите спойлер..."
              />
            </div>
          </EditorContextMenu>
        </Slate>
      </div>
      <div className="hidden">{children}</div>

      <LinkModal
        open={linkModalOpen}
        onOpenChange={setLinkModalOpen}
        initialData={{ name: "", url: "", isEditing: false }}
        onSubmit={handleLinkSubmit}
      />
    </div>
  );
}
