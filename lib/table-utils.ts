import {
  Editor,
  Transforms,
  Element as SlateElement,
  Node,
  Range,
  Path,
} from "slate";
import type { CustomElement } from "@/types/editor";

// ── Structural guards ────────────────────────────────────────────────────
// Tables are a grid, not a document tree — Slate's default merge/split
// logic has no concept of "another cell" or "another row", so every
// structural node (table / table-row / table-cell) is protected from
// being silently merged, split, or removed by anything other than the
// explicit helpers below.

const TABLE_STRUCTURAL_TYPES = new Set(["table", "table-row", "table-cell"]);

function isStructuralTableNode(node: Node | undefined): boolean {
  return (
    !!node &&
    SlateElement.isElement(node) &&
    TABLE_STRUCTURAL_TYPES.has((node as any).type)
  );
}

const bypassTableOps = new WeakSet<Node>();
export function bypassTableNode(node: Node) {
  bypassTableOps.add(node);
}

// ── Node factories ───────────────────────────────────────────────────────

function createEmptyCell(isHeader: boolean): CustomElement {
  return {
    type: "table-cell",
    header: isHeader,
    align: null,
    children: [{ text: "" }],
  } as CustomElement;
}

function createTableRow(colCount: number, isHeader: boolean): CustomElement {
  return {
    type: "table-row",
    header: isHeader,
    children: Array.from({ length: colCount }, () => createEmptyCell(isHeader)),
  } as CustomElement;
}

function createTable(rowCount: number, colCount: number): CustomElement {
  return {
    type: "table",
    children: Array.from({ length: rowCount }, (_, rowIdx) =>
      createTableRow(colCount, rowIdx === 0),
    ),
  } as CustomElement;
}

// ── Enter-in-cell navigation ─────────────────────────────────────────────
// Enter inside a cell moves the selection to the cell directly below it
// (same column). If there is no row below, a new row is created first
// (matching the current row's column count) and the selection lands in
// its cell at that same column index.

function moveToCellBelowOrInsertRow(editor: Editor, cellPath: Path) {
  const rowPath = Path.parent(cellPath);
  const nextRowPath = Path.next(rowPath);
  const colIndex = cellPath[cellPath.length - 1];

  if (Node.has(editor, nextRowPath)) {
    const nextRowNode = Node.get(editor, nextRowPath) as CustomElement;
    const targetColIndex = Math.min(colIndex, nextRowNode.children.length - 1);
    const targetCellPath = [...nextRowPath, targetColIndex];
    Transforms.select(editor, Editor.start(editor, targetCellPath));
    return;
  }

  const rowNode = Node.get(editor, rowPath) as CustomElement;
  const colCount = rowNode.children.length;
  const newRow = createTableRow(colCount, false);

  Transforms.insertNodes(editor, newRow, { at: nextRowPath });

  const targetCellPath = [...nextRowPath, colIndex];
  Transforms.select(editor, Editor.start(editor, targetCellPath));
}

// ── Slate plugin ─────────────────────────────────────────────────────────

export const withTables = (editor: Editor) => {
  const { deleteBackward, deleteForward, deleteFragment, insertBreak, apply } =
    editor;

  // --- Command-level: clean no-op instead of falling into Slate's default
  // merge logic, which treats "previous sibling" inside a row as just
  // another block to fuse into — it has no concept of "another cell". ---

  editor.deleteBackward = (unit) => {
    const { selection } = editor;
    if (selection && Range.isCollapsed(selection)) {
      const cellEntry = Editor.above(editor, {
        match: (n) =>
          SlateElement.isElement(n) && (n as any).type === "table-cell",
      });
      if (cellEntry) {
        const [, cellPath] = cellEntry;
        const cellStart = Editor.start(editor, cellPath);
        if (
          Path.equals(selection.anchor.path, cellStart.path) &&
          selection.anchor.offset === cellStart.offset
        ) {
          return; // at the very start of a cell — block, don't merge into the previous cell/row
        }
      }
    }
    deleteBackward(unit);
  };

  editor.deleteForward = (unit) => {
    const { selection } = editor;
    if (selection && Range.isCollapsed(selection)) {
      const cellEntry = Editor.above(editor, {
        match: (n) =>
          SlateElement.isElement(n) && (n as any).type === "table-cell",
      });
      if (cellEntry) {
        const [, cellPath] = cellEntry;
        const cellEnd = Editor.end(editor, cellPath);
        if (
          Path.equals(selection.anchor.path, cellEnd.path) &&
          selection.anchor.offset === cellEnd.offset
        ) {
          return; // at the very end of a cell — block
        }
      }
    }
    deleteForward(unit);
  };

  // Drag-selected range spanning more than one cell (or crossing the
  // table boundary) is ambiguous for a grid — block rather than guess.
  // A range confined to one cell's own text falls through as normal.
  editor.deleteFragment = (direction) => {
    const { selection } = editor;
    if (selection && !Range.isCollapsed(selection)) {
      const [start, end] = Range.edges(selection);
      const startCell = Editor.above(editor, {
        at: start,
        match: (n) =>
          SlateElement.isElement(n) && (n as any).type === "table-cell",
      });
      const endCell = Editor.above(editor, {
        at: end,
        match: (n) =>
          SlateElement.isElement(n) && (n as any).type === "table-cell",
      });
      if (!!startCell !== !!endCell) return; // crosses table boundary
      if (startCell && endCell && !Path.equals(startCell[1], endCell[1]))
        return; // spans multiple cells
    }
    deleteFragment(direction);
  };

  // Enter inside a cell: move to the cell below (same column), creating
  // a new row first if one doesn't exist yet.
  editor.insertBreak = () => {
    const cellEntry = Editor.above(editor, {
      match: (n) =>
        SlateElement.isElement(n) && (n as any).type === "table-cell",
    });
    if (cellEntry) {
      const [, cellPath] = cellEntry;
      moveToCellBelowOrInsertRow(editor, cellPath);
      return;
    }
    insertBreak();
  };

  // --- Apply-level safety net (same role as withInlineEditors' apply
  // override): blocks structural corruption no matter which command path
  // triggered it. merge_node is the one that actually matters here — it's
  // the operation behind "column not really deleting" and "tr escaping". ---
  editor.apply = (operation) => {
    if (
      operation.type === "remove_node" &&
      isStructuralTableNode(operation.node)
    ) {
      if (bypassTableOps.has(operation.node)) {
        bypassTableOps.delete(operation.node);
        apply(operation);
        return;
      }
      return;
    }

    if (operation.type === "merge_node" || operation.type === "split_node") {
      const target = Node.get(editor, operation.path);
      if (isStructuralTableNode(target)) {
        if (bypassTableOps.has(target)) {
          bypassTableOps.delete(target);
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

// ── Table helpers ────────────────────────────────────────────────────────

export const getTableCellAtSelection = (editor: Editor) => {
  const { selection } = editor;
  if (!selection) return null;
  const [cellEntry] = Editor.nodes(editor, {
    at: selection,
    match: (n) =>
      !Editor.isEditor(n) &&
      SlateElement.isElement(n) &&
      n.type === "table-cell",
  });
  return cellEntry || null;
};

export function deleteTableRow(
  editor: Editor,
  tablePath: Path,
  rowIndex: number,
) {
  const tableNode = Node.get(editor, tablePath) as any;
  if (tableNode.children.length <= 1) return; // keep at least one row

  const rowNode = Node.get(editor, [...tablePath, rowIndex]);
  bypassTableNode(rowNode); // must happen before removeNodes, or withTables' apply guard blocks it
  Transforms.removeNodes(editor, { at: [...tablePath, rowIndex] });
}

export function deleteTableColumn(
  editor: Editor,
  tablePath: Path,
  colIndex: number,
) {
  const tableNode = Node.get(editor, tablePath) as any;
  const colCount = tableNode.children[0]?.children?.length ?? 0;
  if (colCount <= 1) return; // keep at least one column

  // Top-to-bottom is safe here: removing cell `colIndex` from row 0 doesn't
  // touch row 1's children array at all — each row's cells are independent,
  // unlike removing multiple *rows* (where indices would shift on you).
  for (let rowIdx = 0; rowIdx < tableNode.children.length; rowIdx++) {
    const cellNode = Node.get(editor, [...tablePath, rowIdx, colIndex]);
    bypassTableNode(cellNode);
    Transforms.removeNodes(editor, { at: [...tablePath, rowIdx, colIndex] });
  }
}

export function deleteTable(editor: Editor, tablePath: Path) {
  const tableNode = Node.get(editor, tablePath);
  bypassTableNode(tableNode);
  Transforms.removeNodes(editor, { at: tablePath });
}

// ── Table insertion ──────────────────────────────────────────────────────
// Same insertion pattern as insertInlineEditor: insert the node at the
// current selection, then move past it. Row 0 is marked as a header row
// (matching table-cell-view.tsx's schema) so serializeToMarkdown emits the
// required "| --- |" separator line under it.

export const insertTable = (editor: Editor, rows = 2, cols = 2) => {
  const table = createTable(rows, cols);
  Transforms.insertNodes(editor, table);
  Transforms.move(editor);
};
