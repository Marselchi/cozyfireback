"use client";

import { Children } from "react";
import type { Editor } from "slate";

type TableViewProps = {
  attributes: Record<string, any>;
  element: any;
  editor: Editor;
  children: React.ReactNode;
};

// Grouping into thead/tbody is required for valid HTML — a bare <tr> can't
// be a direct child of <table>, only thead/tbody/tfoot/caption/colgroup can.
export function TableView({ attributes, element, children }: TableViewProps) {
  const rowsData = element.children ?? [];
  const childArray = Children.toArray(children);
  const headerChildren = childArray.filter((_, i) => rowsData[i]?.header);
  const bodyChildren = childArray.filter((_, i) => !rowsData[i]?.header);

  return (
    <div {...attributes} className="relative my-2">
      <table className="min-w-20 border-collapse border border-gray-300 table-auto">
        {headerChildren.length > 0 && <thead>{headerChildren}</thead>}
        <tbody>{bodyChildren}</tbody>
      </table>
    </div>
  );
}
