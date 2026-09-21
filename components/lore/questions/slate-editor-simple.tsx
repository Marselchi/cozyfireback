"use client";
import { useState, useMemo, useCallback, useRef, useEffect } from "react";
import {
  createEditor,
  type Descendant,
  type NodeEntry,
  Transforms,
  Editor,
  Element as SlateElement,
} from "slate";
import { Slate, Editable, withReact } from "slate-react";
import { withHistory, HistoryEditor } from "slate-history";
import { type EditorMode, emptyContent } from "@/types/editor";
import {
  withShortcuts,
  parseMarkdownToSlate,
  serializeToMarkdown,
  withBlockEnter,
} from "@/lib/editor-utils";
import { decorateMarkdown } from "@/lib/markdown-decorator";
import { cn } from "@/lib/utils";
import { EditorContextMenu } from "@/components/context-menu/context-menu";
import { ElementRenderer } from "@/components/editor/element-renderer";
import { LeafRenderer } from "@/components/editor/leaf-renderer";
import { Toolbar } from "@/components/toolbar";
import { mainPreset, simplePreset } from "@/lib/toolbar/presets";

type SlateEditorSimpleProps = {
  initialContent?: Descendant[];
  onChange: (value: Descendant[]) => void;
};

// Конвертеры оставлены локально, так как в простой версии есть специфика обработки inline-editor
const convertParsedToRaw = (nodes: Descendant[]): Descendant[] => {
  const result: Descendant[] = [];
  for (const node of nodes) {
    if (SlateElement.isElement(node) && node.type === "inline-editor") {
      result.push(node);
    } else {
      const markdown = serializeToMarkdown([node]);
      if (markdown) {
        result.push({
          type: "paragraph" as const,
          children: [{ text: markdown }],
        });
      }
    }
  }
  return result.length > 0
    ? result
    : [{ type: "paragraph" as const, children: [{ text: "" }] }];
};

const convertRawToParsed = (nodes: Descendant[]): Descendant[] => {
  const result: Descendant[] = [];
  const textLines: string[] = [];
  for (const node of nodes) {
    if (SlateElement.isElement(node) && node.type === "inline-editor") {
      if (textLines.length > 0) {
        const parsed = parseMarkdownToSlate(textLines.join("\n"));
        result.push(...parsed);
        textLines.length = 0;
      }
      result.push(node);
    } else if (SlateElement.isElement(node) && "children" in node) {
      const text = (node.children as any[]).map((c) => c.text || " ").join(" ");
      textLines.push(text);
    }
  }
  if (textLines.length > 0) {
    const parsed = parseMarkdownToSlate(textLines.join("\n"));
    result.push(...parsed);
  }
  return result.length > 0
    ? result
    : [{ type: "paragraph" as const, children: [{ text: "" }] }];
};

export function SlateEditorSimple({
  initialContent,
  onChange,
}: Readonly<SlateEditorSimpleProps>) {
  const [mode, setMode] = useState<EditorMode>("parsed");
  const modeRef = useRef<EditorMode>("parsed");
  modeRef.current = mode;

  const editor = useMemo(() => {
    const e = withHistory(withReact(createEditor()));
    return withBlockEnter(withShortcuts(e, () => modeRef.current === "parsed"));
  }, []);

  // Новая архитектура: editor.children — единственный источник правды.
  // Синхронизируем внешние изменения initialContent напрямую в редактор.
  useEffect(() => {
    const nextContent = initialContent ?? emptyContent;
    HistoryEditor.withoutSaving(editor, () => {
      Transforms.deselect(editor);
      editor.children = nextContent;
    });
    editor.history = { undos: [], redos: [] };
    editor.onChange();
    setMode("parsed");
  }, [editor]);

  const handleValueChange = useCallback(
    (newValue: Descendant[]) => {
      onChange(newValue);
    },
    [onChange],
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
      const nextValue = converted.length > 0 ? converted : emptyContent;

      // Прямая мутация editor.children без дублирования в React state
      editor.children = nextValue;
      Transforms.select(editor, Editor.start(editor, []));
      editor.onChange();
      setMode(newMode);
    },
    [mode, editor],
  );

  const renderElement = useCallback(
    (props: any) => <ElementRenderer {...props} mode={mode} />,
    [mode],
  );

  const renderLeaf = useCallback(
    (props: any) => <LeafRenderer {...props} />,
    [],
  );

  const decorate = useCallback(
    (entry: NodeEntry) => {
      if (mode === "raw") {
        const [node] = entry;
        if (SlateElement.isElement(node) && node.type === "inline-editor") {
          return [];
        }
        return decorateMarkdown(entry);
      }
      return [];
    },
    [mode],
  );

  return (
    <div className="space-y-4">
      <div className="rounded-lg rounded-t-none border border-border shadow-sm">
        <Slate
          editor={editor}
          initialValue={initialContent ?? emptyContent}
          onValueChange={handleValueChange}
        >
          <Toolbar
            mode={mode}
            onModeChange={handleModeChange}
            preset={simplePreset}
          />
          <EditorContextMenu
            preset={mainPreset}
            showInlineEditorOption={false}
            isInsideInlineEditor={false}
            mode={mode}
            showGeneratorOption={false}
          >
            <Editable
              lang="ru"
              className={cn(
                "max-h-96 overflow-y-auto rounded-b-lg bg-background p-4 focus:outline-none overflow-x-hidden",
              )}
              renderElement={renderElement}
              renderLeaf={renderLeaf}
              decorate={decorate}
              placeholder="Начните писать..."
              spellCheck
              autoFocus
            />
          </EditorContextMenu>
        </Slate>
      </div>
    </div>
  );
}
