"use client";

import { useState, useMemo, useCallback, useRef, useEffect } from "react";
import {
  createEditor,
  type Descendant,
  type NodeEntry,
  Transforms,
  Editor,
} from "slate";
import { Slate, Editable, withReact } from "slate-react";
import { withHistory, HistoryEditor } from "slate-history";
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
  DragOverlay,
} from "@dnd-kit/core";
import { Toolbar } from "../toolbar";
import { ElementRenderer } from "./element-renderer";
import { LeafRenderer } from "./leaf-renderer";
import { EditorContextMenu } from "../context-menu";
import type { EditorMode, EditorContent } from "@/types/editor";
import { emptyContent } from "@/types/editor";
import {
  withShortcuts,
  withInlineEditors,
  withBlockEnter,
  moveInlineEditor,
  withTables,
} from "@/lib/editor-utils";
import {
  convertParsedToRaw,
  convertRawToParsed,
  decorateMarkdown,
} from "@/lib/editor";
import { cn } from "@/lib/utils";
import { useEditorState } from "./editor-state-provider";
import { InsideInlineEditorContext } from "./inline-editor-dnd-wrapper";
import { ToolbarPreset } from "@/lib/toolbar/types";
import { mainPreset } from "@/lib/toolbar/presets";
import { mainPreset as mainContextPreset } from "@/lib/context-menu/presets";
import { useShallow } from "zustand/react/shallow";

type SlateEditorProps = {
  initialContent?: EditorContent;
  variant?: ToolbarPreset;
};

const EMPTY_CONTENT: Descendant[] = emptyContent;

export function SlateEditor({
  initialContent,
  variant,
}: Readonly<SlateEditorProps>) {
  const { setPreviewValueDebounced, resetPreviewStore } = useEditorState(
    useShallow((s) => ({
      setPreviewValueDebounced: s.setPreviewValueDebounced,
      resetPreviewStore: s.resetPreviewStore,
    })),
  );
  const [mode, setMode] = useState<EditorMode>("parsed");

  const modeRef = useRef<EditorMode>("parsed");
  modeRef.current = mode;

  const editor = useMemo(() => {
    const e = withHistory(withReact(createEditor()));
    return withBlockEnter(
      withTables(
        withShortcuts(withInlineEditors(e), () => modeRef.current === "parsed"),
      ),
    );
  }, []);

  const initialEditorContent = initialContent?.content ?? EMPTY_CONTENT;
  const initialKey =
    initialContent?.id !== undefined ? `draft-${initialContent.id}` : "empty";

  // NOTE: editor.children is the single source of truth for content.
  // We no longer mirror it into parsedValue/rawValue React state — that
  // state was only ever fed into Slate's `initialValue` prop, which
  // slate-react reads exactly once (on mount / on `key`-forced remount).
  // Updating it on every keystroke did nothing useful, and updating it
  // when `initialContent.content` changed *without* the id changing was
  // actively broken: no remount happens in that case, so the prop update
  // was silently ignored and the editor never reflected the new content.
  //
  // Fix: when new content needs to be loaded and we're NOT going through
  // a remount (same id, content changed underneath us), write directly
  // into editor.children instead of hoping a prop change is picked up.
  useEffect(() => {
    const nextContent = initialContent?.content ?? EMPTY_CONTENT;

    HistoryEditor.withoutSaving(editor, () => {
      Transforms.deselect(editor);
      editor.children = nextContent;
    });
    editor.history = { undos: [], redos: [] };
    editor.onChange();

    setMode("parsed");

    // Самое важное: синхронизируем provider с тем, что реально показано в редакторе
    resetPreviewStore(nextContent);
  }, [initialKey, resetPreviewStore, initialContent?.content, editor]);

  const handleValueChange = useCallback(
    (newValue: Descendant[]) => {
      // Preview must always reflect the *parsed* (semantic) tree, never the
      // raw one-line-per-paragraph editing structure — otherwise it shows
      // literal "**"/"_" characters with no styling, and splits soft-broken
      // lines into separate paragraphs instead of joining them per the
      // markdown spec.
      const previewValue =
        mode === "raw" ? convertRawToParsed(newValue) : newValue;
      setPreviewValueDebounced(previewValue, 180);
    },
    [mode, setPreviewValueDebounced],
  );

  const handleModeChange = useCallback(
    (newMode: EditorMode) => {
      if (newMode === mode) return;

      HistoryEditor.withoutSaving(editor, () => {
        Transforms.deselect(editor);
      });
      editor.history = { undos: [], redos: [] };

      const currentContent = editor.children as Descendant[];
      const converted =
        newMode === "raw"
          ? convertParsedToRaw(currentContent)
          : convertRawToParsed(currentContent);
      const nextValue = converted.length > 0 ? converted : EMPTY_CONTENT;

      // Mutate editor.children directly — this IS the source of truth.
      // There is no parallel React state to keep in sync anymore.
      editor.children = nextValue;
      Transforms.select(editor, Editor.start(editor, []));
      editor.onChange();

      setMode(newMode);
    },
    [mode, editor],
  );

  const renderElement = useCallback(
    (props: any) => (
      <ElementRenderer
        {...props}
        mode={mode}
        roles={initialContent?.roles ?? []}
      />
    ),
    [mode, initialContent?.roles],
  );

  const renderLeaf = useCallback(
    (props: any) => <LeafRenderer {...props} />,
    [],
  );

  const decorate = useCallback(
    (entry: NodeEntry) => (mode === "raw" ? decorateMarkdown(entry) : []),
    [mode],
  );

  const handleDragEnd = useCallback(
    (event: DragEndEvent) => {
      const { active, over } = event;

      if (active.data?.current?.type === "inline-editor" && over) {
        try {
          const inlineEditorId = active.id as string;

          let targetPath: number[];
          if (typeof over.id === "string") {
            try {
              const parsedPath = JSON.parse(over.id as string);
              targetPath = Array.isArray(parsedPath) ? parsedPath : [0];
            } catch {
              targetPath = [0];
            }
          } else if (Array.isArray(over.id)) {
            targetPath = over.id as number[];
          } else {
            targetPath = [over.id as number];
          }

          for (let depth = targetPath.length - 1; depth >= 0; depth--) {
            try {
              const [parentNode] = Editor.node(
                editor,
                targetPath.slice(0, depth),
              );
              if (
                parentNode &&
                typeof parentNode === "object" &&
                "type" in parentNode &&
                parentNode.type === "inline-editor"
              ) {
                return;
              }
            } catch {
              continue;
            }
          }

          moveInlineEditor(editor, inlineEditorId, targetPath);
          editor.onChange();
        } catch (e) {
          console.error("Error handling drag end in main editor", e);
        }
      }
    },
    [editor],
  );

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor),
  );

  const customCollisionDetection = useCallback(
    (args: any) => {
      const collisions = closestCenter(args);

      return collisions.filter((collision: any) => {
        try {
          let targetPath: number[];
          if (typeof collision.id === "string") {
            try {
              const parsedPath = JSON.parse(collision.id as string);
              targetPath = Array.isArray(parsedPath) ? parsedPath : [0];
            } catch {
              return true;
            }
          } else if (Array.isArray(collision.id)) {
            targetPath = collision.id as number[];
          } else {
            targetPath = [collision.id as number];
          }

          for (let depth = targetPath.length - 1; depth >= 0; depth--) {
            try {
              const [parentNode] = Editor.node(
                editor,
                targetPath.slice(0, depth),
              );
              if (
                parentNode &&
                typeof parentNode === "object" &&
                "type" in parentNode &&
                parentNode.type === "inline-editor"
              ) {
                return false;
              }
            } catch {
              continue;
            }
          }

          return true;
        } catch {
          return true;
        }
      });
    },
    [editor],
  );

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={customCollisionDetection}
      onDragEnd={handleDragEnd}
    >
      <div className="space-y-4">
        <div className="relative rounded-lg rounded-t-none border border-border shadow-sm editor-styles">
          <Slate
            key={initialKey}
            editor={editor}
            initialValue={initialEditorContent}
            onValueChange={handleValueChange}
          >
            <div className="sticky top-0 z-10">
              <Toolbar
                mode={mode}
                onModeChange={handleModeChange}
                preset={variant ?? mainPreset}
              />
            </div>
            <InsideInlineEditorContext.Provider value={false}>
              <EditorContextMenu
                preset={mainContextPreset}
                showInlineEditorOption={true}
                isInsideInlineEditor={false}
                mode={mode}
              >
                <Editable
                  lang="ru"
                  className={cn(
                    "min-h-100 rounded-b-lg bg-background p-4 focus:outline-none",
                  )}
                  renderElement={renderElement}
                  renderLeaf={renderLeaf}
                  decorate={decorate}
                  placeholder={
                    mode === "raw"
                      ? "Макрдаун... **жирный**, _курсив_, # заголовки"
                      : "Начните писать... Используйте # для заголовков, > для цитат"
                  }
                  spellCheck
                  autoFocus
                />
              </EditorContextMenu>
            </InsideInlineEditorContext.Provider>
          </Slate>
        </div>
      </div>
      <DragOverlay>
        <div className="bg-foreground/20 border-2 border-amber-400 rounded-lg p-4 shadow-lg pointer-events-none w-40">
          <div className="text-sm text-muted-foreground">
            Выберите строку для перемещения
          </div>
        </div>
      </DragOverlay>
    </DndContext>
  );
}
