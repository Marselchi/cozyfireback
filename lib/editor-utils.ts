import {
  Editor,
  Transforms,
  Element as SlateElement,
  type Descendant,
  Range,
  Point,
  Path,
} from "slate";
import type {
  CustomElement,
  CustomText,
  BulletedListElement,
  InlineEditorElement,
  LinkElement,
  EditorMode,
  SpoilerType,
  ChanceMetadata,
} from "@/types/editor";

const bypassRemoval = new WeakSet();

// ── Mark helpers ──────────────────────────────────────────────────────────

export const isMarkActive = (
  editor: Editor,
  format: keyof Omit<CustomText, "text">,
) => {
  if (!editor.selection) return false;
  try {
    const marks = Editor.marks(editor);
    return marks ? marks[format] === true : false;
  } catch {
    return false;
  }
};

export const toggleMark = (
  editor: Editor,
  format: keyof Omit<CustomText, "text">,
) => {
  const isActive = isMarkActive(editor, format);
  if (isActive) {
    Editor.removeMark(editor, format);
  } else {
    Editor.addMark(editor, format, true);
  }
};

// ── Block helpers ─────────────────────────────────────────────────────────

export const isBlockActive = (
  editor: Editor,
  format: CustomElement["type"],
) => {
  const { selection } = editor;
  if (!selection) return false;

  // Считаем blockquote и quote-block одним форматом для тулбара
  const formatsToCheck =
    format === "blockquote" ? ["blockquote", "quote-block"] : [format];

  const [match] = Array.from(
    Editor.nodes(editor, {
      at: Editor.unhangRange(editor, selection),
      match: (n) =>
        !Editor.isEditor(n) &&
        SlateElement.isElement(n) &&
        formatsToCheck.includes(n.type as any),
    }),
  );
  return !!match;
};

export const toggleBlock = (editor: Editor, format: CustomElement["type"]) => {
  const isActive = isBlockActive(editor, format);
  const isList = format === "bulleted-list" || format === "numbered-list";

  Transforms.unwrapNodes(editor, {
    match: (n) =>
      !Editor.isEditor(n) &&
      SlateElement.isElement(n) &&
      (n.type === "bulleted-list" || n.type === "numbered-list"),
    split: true,
  });

  let newProperties: Partial<CustomElement> = {
    type: isActive ? "paragraph" : isList ? "list-item" : format,
  } as Partial<CustomElement>;

  // Исправление ошибки TS: пересоздаем объект, чтобы добавить level
  if (format === "heading" && !isActive) {
    newProperties = {
      ...newProperties,
      level: 2,
    } as Partial<CustomElement>;
  }

  // ВАЖНО: Добавлен match, чтобы свойства применялись к самому блоку, а не к тексту внутри него!
  Transforms.setNodes<CustomElement>(editor, newProperties, {
    match: (n) => SlateElement.isElement(n) && Editor.isBlock(editor, n),
  });

  if (!isActive && isList) {
    const block = { type: format, children: [] } as CustomElement;
    Transforms.wrapNodes(editor, block);
  }
};

// ── Inline editor management ──────────────────────────────────────────────

export const insertInlineEditor = (editor: Editor) => {
  const id = `inline-${Date.now()}`;
  const inlineEditor: CustomElement = {
    type: "inline-editor",
    id,
    roles: [],
    spoilerType: "normal",
    content: [{ type: "paragraph", children: [{ text: "" }] }],
    children: [{ text: "" }],
  };

  Transforms.insertNodes(editor, inlineEditor);
  Transforms.move(editor);
};

export const removeInlineEditor = (editor: Editor, id?: string) => {
  if (!id) return;

  const entry = Array.from(
    Editor.nodes(editor, {
      at: [],
      match: (n) =>
        SlateElement.isElement(n) &&
        n.type === "inline-editor" &&
        (n as any).id === id,
    }),
  )[0];

  if (!entry) return;

  const [node, path] = entry;
  bypassRemoval.add(node);
  Transforms.removeNodes(editor, { at: path });
};

export const updateInlineEditorContent = (
  editor: Editor,
  id: string | undefined,
  content: Descendant[],
) => {
  if (!id) return;

  const [match] = Editor.nodes(editor, {
    at: [],
    match: (n) =>
      SlateElement.isElement(n) &&
      n.type === "inline-editor" &&
      (n as any).id === id,
  });

  if (match) {
    const [, path] = match;
    Transforms.setNodes(editor, { content } as Partial<CustomElement>, {
      at: path,
    });
  }
};

export const updateInlineEditorRoles = (
  editor: Editor,
  id: string | undefined,
  roles: string[],
) => {
  if (!id) return;

  const [match] = Editor.nodes(editor, {
    at: [],
    match: (n) =>
      SlateElement.isElement(n) &&
      n.type === "inline-editor" &&
      (n as any).id === id,
  });

  if (match) {
    const [, path] = match;
    Transforms.setNodes(editor, { roles } as Partial<CustomElement>, {
      at: path,
    });
  }
};

export const updateInlineEditorSpoiler = (
  editor: Editor,
  id: string | undefined,
  patch: { spoilerType: SpoilerType; chance?: ChanceMetadata },
) => {
  if (!id) return;

  const [match] = Editor.nodes(editor, {
    at: [],
    match: (n) =>
      SlateElement.isElement(n) &&
      n.type === "inline-editor" &&
      (n as any).id === id,
  });

  if (match) {
    const [, path] = match;
    Transforms.setNodes(
      editor,
      {
        spoilerType: patch.spoilerType,
        chance: patch.chance,
      } as Partial<CustomElement>,
      { at: path },
    );
  }
};

export const moveInlineEditor = (
  editor: Editor,
  id: string,
  newPath: number[],
) => {
  const [match] = Editor.nodes(editor, {
    at: [],
    match: (n) =>
      SlateElement.isElement(n) &&
      n.type === "inline-editor" &&
      (n as any).id === id,
  });

  if (match) {
    const [, oldPath] = match;
    Transforms.moveNodes(editor, { at: oldPath, to: newPath });
  }
};

// ── Link helpers ──────────────────────────────────────────────────────────

export const isLinkActive = (editor: Editor) => {
  const [link] = Editor.nodes(editor, {
    match: (n) =>
      !Editor.isEditor(n) && SlateElement.isElement(n) && n.type === "link",
  });
  return !!link;
};

export const unwrapLink = (editor: Editor) => {
  Transforms.unwrapNodes(editor, {
    match: (n) =>
      !Editor.isEditor(n) && SlateElement.isElement(n) && n.type === "link",
  });
};

export const wrapLink = (editor: Editor, url: string) => {
  if (isLinkActive(editor)) unwrapLink(editor);

  const { selection } = editor;
  const isCollapsed = selection && Range.isCollapsed(selection);

  const link: LinkElement = {
    type: "link",
    url,
    children: isCollapsed ? [{ text: url }] : [],
  };

  if (isCollapsed) {
    Transforms.insertNodes(editor, link);
  } else {
    Transforms.wrapNodes(editor, link, { split: true });
    Transforms.collapse(editor, { edge: "end" });
  }
};

export const insertLink = (
  editor: Editor,
  url: string,
  text?: string,
  hasSelection = false,
  mode: EditorMode = "parsed",
) => {
  if (
    hasSelection &&
    editor.selection &&
    !Range.isCollapsed(editor.selection)
  ) {
    wrapLink(editor, url);
  } else if (mode === "raw") {
    const markdownLink = `[${text || url}](${url})`;
    Transforms.insertText(editor, markdownLink);
    Transforms.collapse(editor, { edge: "end" });
  } else {
    const link: LinkElement = {
      type: "link",
      url,
      children: [{ text: text || url }],
    };
    Transforms.insertNodes(editor, link);
    Transforms.collapse(editor, { edge: "end" });
  }
};

export const getLinkAtSelection = (editor: Editor) => {
  const { selection } = editor;
  if (!selection) return null;

  const [linkEntry] = Editor.nodes(editor, {
    at: selection,
    match: (n) =>
      !Editor.isEditor(n) && SlateElement.isElement(n) && n.type === "link",
  });

  return linkEntry || null;
};

// ── Table support (registry) ─────────────────────────────────────────────
// All table-specific Slate logic lives in table-utils.ts; re-exported here
// so editor-utils remains the single entry point consumers import from.

export {
  withTables,
  bypassTableNode,
  getTableCellAtSelection,
  deleteTableRow,
  deleteTableColumn,
  deleteTable,
  insertTable,
} from "./table-utils";

// ── Slate plugins ─────────────────────────────────────────────────────────

const BLOCK_SHORTCUTS: Record<
  string,
  CustomElement["type"] | { type: "heading"; level: number }
> = {
  "*": "list-item",
  "-": "list-item",
  "+": "list-item",
  ">": "blockquote",
  "<?": "quote-block",
  "#": { type: "heading", level: 1 },
  "##": { type: "heading", level: 2 },
  "###": { type: "heading", level: 3 },
  "####": { type: "heading", level: 4 },
  "#####": { type: "heading", level: 5 },
  "######": { type: "heading", level: 6 },
  "```": "code-block",
};

export const withInlineEditors = (editor: Editor) => {
  const { isVoid, isInline, deleteBackward, deleteForward, apply } = editor;

  editor.isVoid = (element) =>
    element.type === "inline-editor" || element.type === "dice"
      ? true
      : isVoid(element);

  editor.isInline = (element) => {
    if (element.type === "link" || element.type === "dice") return true;
    return element.type === "inline-editor" ? false : isInline(element);
  };

  editor.deleteBackward = (unit) => {
    const { selection } = editor;
    if (selection && Range.isCollapsed(selection)) {
      const prevEntry = Editor.previous(editor, { at: selection });
      if (prevEntry) {
        const [prevNode] = prevEntry;
        if (
          SlateElement.isElement(prevNode) &&
          prevNode.type === "inline-editor"
        ) {
          return;
        }
      }
    }
    deleteBackward(unit);
  };

  editor.deleteForward = (unit) => {
    const { selection } = editor;
    if (selection && Range.isCollapsed(selection)) {
      const nextEntry = Editor.next(editor, { at: selection });
      if (nextEntry) {
        const [nextNode] = nextEntry;
        if (
          SlateElement.isElement(nextNode) &&
          nextNode.type === "inline-editor"
        ) {
          return;
        }
      }
    }
    deleteForward(unit);
  };

  editor.apply = (operation) => {
    if (operation.type === "remove_node") {
      const node = operation.node;
      if (SlateElement.isElement(node) && node.type === "inline-editor") {
        if (bypassRemoval.has(node)) {
          bypassRemoval.delete(node);
          apply(operation);
          return;
        }
        return;
      }
    }
    apply(operation);
  };

  return editor;
};

export const withShortcuts = (editor: Editor, enabled: () => boolean) => {
  const { deleteBackward, insertText, isInline, isVoid } = editor;

  editor.isVoid = (element) =>
    element.type === "dice" ? true : isVoid(element);

  editor.isInline = (element) => {
    if (element.type === "link" || element.type === "dice") return true;
    return isInline(element);
  };

  editor.insertText = (text) => {
    if (!enabled()) {
      insertText(text);
      return;
    }

    const { selection } = editor;

    if (selection && Range.isCollapsed(selection)) {
      const { anchor } = selection;
      const block = Editor.above(editor, {
        match: (n) => SlateElement.isElement(n) && Editor.isBlock(editor, n),
      });
      const path = block ? block[1] : [];
      const start = Editor.start(editor, path);
      const range = { anchor, focus: start };
      const beforeText = Editor.string(editor, range);

      if (text === " ") {
        const shortcut = BLOCK_SHORTCUTS[beforeText];

        if (shortcut) {
          Transforms.select(editor, range);
          if (!Range.isCollapsed(range)) Transforms.delete(editor);

          if (typeof shortcut === "object" && shortcut.type === "heading") {
            Transforms.setNodes<SlateElement>(
              editor,
              {
                type: "heading",
                level: shortcut.level as 1 | 2 | 3 | 4 | 5 | 6,
              },
              {
                match: (n) =>
                  SlateElement.isElement(n) && Editor.isBlock(editor, n),
              },
            );
          } else {
            const type = shortcut as CustomElement["type"];
            Transforms.setNodes<SlateElement>(
              editor,
              { type } as Partial<SlateElement>,
              {
                match: (n) =>
                  SlateElement.isElement(n) && Editor.isBlock(editor, n),
              },
            );
            if (type === "list-item") {
              const list: BulletedListElement = {
                type: "bulleted-list",
                children: [],
              };
              Transforms.wrapNodes(editor, list, {
                match: (n) =>
                  !Editor.isEditor(n) &&
                  SlateElement.isElement(n) &&
                  n.type === "list-item",
              });
            }
          }
          return;
        }
      }
    }

    insertText(text);
  };

  editor.deleteBackward = (...args) => {
    if (!enabled()) {
      deleteBackward(...args);
      return;
    }

    const { selection } = editor;

    if (selection && Range.isCollapsed(selection)) {
      const match = Editor.above(editor, {
        match: (n) => SlateElement.isElement(n) && Editor.isBlock(editor, n),
      });

      if (match) {
        const [block, path] = match;
        const start = Editor.start(editor, path);

        if (
          !Editor.isEditor(block) &&
          SlateElement.isElement(block) &&
          block.type !== "paragraph" &&
          block.type !== "inline-editor" &&
          Point.equals(selection.anchor, start)
        ) {
          Transforms.setNodes(editor, {
            type: "paragraph",
          } as Partial<SlateElement>);

          if (block.type === "list-item") {
            Transforms.unwrapNodes(editor, {
              match: (n) =>
                !Editor.isEditor(n) &&
                SlateElement.isElement(n) &&
                (n.type === "bulleted-list" || n.type === "numbered-list"),
              split: true,
            });
          }
          return;
        }
      }
    }

    deleteBackward(...args);
  };

  return editor;
};

export const withBlockEnter = (editor: Editor) => {
  const { insertBreak } = editor;
  editor.insertBreak = () => {
    const { selection } = editor;
    if (selection && Range.isCollapsed(selection)) {
      const { anchor } = selection;

      // 1. Code-block: вставляем перенос строки вместо создания нового блока
      const [codeBlockMatch] = Editor.nodes(editor, {
        at: selection,
        match: (n) =>
          !Editor.isEditor(n) &&
          SlateElement.isElement(n) &&
          n.type === "code-block",
      });
      if (codeBlockMatch) {
        Transforms.insertText(editor, "\n");
        return;
      }

      // 2. Link: предотвращаем наследование ссылки на следующей строке
      const [linkMatch] = Editor.nodes(editor, {
        at: selection,
        match: (n) =>
          !Editor.isEditor(n) && SlateElement.isElement(n) && n.type === "link",
      });
      if (linkMatch) {
        const [, linkPath] = linkMatch;
        const isEnd = Editor.isEnd(editor, anchor, linkPath);
        if (isEnd) {
          const [blockMatch] = Editor.nodes(editor, {
            at: selection,
            match: (n) =>
              SlateElement.isElement(n) && Editor.isBlock(editor, n),
          });
          if (blockMatch) {
            const [, blockPath] = blockMatch;
            const isBlockEnd = Editor.isEnd(editor, anchor, blockPath);
            if (isBlockEnd) {
              // Если мы в самом конце блока, создаем новый параграф ПОСЛЕ него (вне ссылки)
              const newPath = Path.next(blockPath);
              Transforms.insertNodes(
                editor,
                { type: "paragraph", children: [{ text: "" }] },
                { at: newPath },
              );
              Transforms.select(editor, newPath);
              Transforms.collapse(editor, { edge: "start" });
              return;
            } else {
              // Если после ссылки есть текст, просто сдвигаем курсор за её пределы
              Transforms.move(editor, { unit: "offset" });
              insertBreak();
              return;
            }
          }
        }
      }

      // 3. Blockquote / Quote-block: выходим из цитаты при нажатии Enter в её конце
      const [quoteMatch] = Editor.nodes(editor, {
        at: selection,
        match: (n) =>
          !Editor.isEditor(n) &&
          SlateElement.isElement(n) &&
          (n.type === "blockquote" || n.type === "quote-block"),
      });
      if (quoteMatch) {
        const [, quotePath] = quoteMatch;
        const isEnd = Editor.isEnd(editor, anchor, quotePath);
        if (isEnd) {
          const [blockMatch] = Editor.nodes(editor, {
            at: selection,
            match: (n) =>
              SlateElement.isElement(n) && Editor.isBlock(editor, n),
          });
          if (blockMatch) {
            const [blockNode] = blockMatch;
            const isEmpty = Editor.isEmpty(editor, blockNode as SlateElement);
            if (isEmpty) {
              // Если строка пустая, просто "разворачиваем" (убираем) цитату
              Transforms.unwrapNodes(editor, {
                match: (n) =>
                  !Editor.isEditor(n) &&
                  SlateElement.isElement(n) &&
                  (n.type === "blockquote" || n.type === "quote-block"),
                split: true,
              });
              return;
            } else {
              // Если в цитате есть текст, создаем новый параграф ПОСЛЕ всей цитаты
              const newPath = Path.next(quotePath);
              Transforms.insertNodes(
                editor,
                { type: "paragraph", children: [{ text: "" }] },
                { at: newPath },
              );
              Transforms.select(editor, newPath);
              Transforms.collapse(editor, { edge: "start" });
              return;
            }
          }
        }
      }
    }
    insertBreak();
  };
  return editor;
};

// ── Serialization (delegated to lib/editor) ───────────────────────────────

export { serializeToMarkdown, serializeToMarkdownPreview } from "@/lib/editor";
export { parseMarkdownToSlate } from "@/lib/editor";
