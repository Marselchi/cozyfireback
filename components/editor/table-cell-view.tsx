"use client";

import { useCallback } from "react";
import { ReactEditor } from "slate-react";
import { Transforms, Node, type Editor } from "slate";

type TableCellViewProps = {
  attributes: Record<string, any>;
  element: any;
  editor: Editor;
  children: React.ReactNode;
};

const PLUS_BASE =
  "absolute z-10 hidden group-hover/cell:flex items-center justify-center " +
  "w-4 h-4 rounded-full bg-blue-500 text-white text-[10px] leading-none shadow " +
  "hover:bg-blue-600";

function makeCell(header: boolean) {
  return { type: "table-cell", header, align: null, children: [{ text: "" }] };
}

export function TableCellView({
  attributes,
  element,
  editor,
  children,
}: TableCellViewProps) {
  const Tag = element.header ? "th" : "td";

  // Resolved fresh on click — never cached, since inserts elsewhere shift paths.
  const getCoords = useCallback(() => {
    const cellPath = ReactEditor.findPath(editor, element);
    const tablePath = cellPath.slice(0, -2);
    return {
      tablePath,
      rowIndex: cellPath[cellPath.length - 2],
      colIndex: cellPath[cellPath.length - 1],
    };
  }, [editor, element]);

  const addRow = useCallback(
    (offset: 0 | 1) => {
      const { tablePath, rowIndex } = getCoords();
      const tableNode = Node.get(editor, tablePath) as any;
      const colCount = tableNode.children[0]?.children?.length ?? 1;
      const newRow = {
        type: "table-row",
        header: false,
        children: Array.from({ length: colCount }, () => makeCell(false)),
      };
      Transforms.insertNodes(editor, newRow as any, {
        at: [...tablePath, rowIndex + offset],
      });
    },
    [editor, getCoords],
  );

  const addColumn = useCallback(
    (offset: 0 | 1) => {
      const { tablePath, colIndex } = getCoords();
      const tableNode = Node.get(editor, tablePath) as any;
      tableNode.children.forEach((row: any, rowIdx: number) => {
        Transforms.insertNodes(editor, makeCell(!!row.header) as any, {
          at: [...tablePath, rowIdx, colIndex + offset],
        });
      });
    },
    [editor, getCoords],
  );

  const stop = (e: React.MouseEvent) => {
    e.preventDefault(); // keep Slate selection from jumping before onClick fires
    e.stopPropagation();
  };

  return (
    <Tag
      {...attributes}
      className="group/cell min-w-20 relative border border-gray-300 px-2 py-1 align-top"
    >
      {children}

      <div contentEditable={false} className="contents">
        {!element.header && (
          <button
            type="button"
            onMouseDown={stop}
            onClick={() => addRow(0)}
            className={`${PLUS_BASE} left-1/2 -top-2 -translate-x-1/2`}
            title="Add row above"
          >
            +
          </button>
        )}
        <button
          type="button"
          onMouseDown={stop}
          onClick={() => addRow(1)}
          className={`${PLUS_BASE} left-1/2 -bottom-2 -translate-x-1/2`}
          title="Add row below"
        >
          +
        </button>
        <button
          type="button"
          onMouseDown={stop}
          onClick={() => addColumn(0)}
          className={`${PLUS_BASE} top-1/2 -left-2 -translate-y-1/2`}
          title="Add column left"
        >
          +
        </button>
        <button
          type="button"
          onMouseDown={stop}
          onClick={() => addColumn(1)}
          className={`${PLUS_BASE} top-1/2 -right-2 -translate-y-1/2`}
          title="Add column right"
        >
          +
        </button>
      </div>
    </Tag>
  );
}
