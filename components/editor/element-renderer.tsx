"use client";

import type React from "react";
import { useCallback, useMemo } from "react";
import type { RenderElementProps } from "slate-react";
import { ReactEditor, useSlate } from "slate-react";
import type { EditorMode } from "@/types/editor";
import {
  InlineEditorDndWrapper,
  useInsideInlineEditor,
} from "./inline-editor-dnd-wrapper";
import { useDroppable } from "@dnd-kit/core";
import { registry } from "@/lib/editor";
import type { Role } from "@/types/editor-layout";
import { classifyLink, linkClassName } from "@/lib/editor/util/link-util";
import { TableView } from "./table-view";
import { TableCellView } from "./table-cell-view";

type ElementRendererProps = RenderElementProps & {
  mode: EditorMode;
  roles?: Role[];
};

// Visual feedback for a valid inline-editor drop target (replaces the old
// `cn(..., isDraggedOver && "ring-2 ring-blue-500")` per-tag className).
// Each node definition's render() hardcodes its own className after
// spreading `{...attributes}`, so a className passed through attributes
// would just get clobbered — `style` isn't set by any definition, so this
// composes cleanly with every node type instead.
const DRAG_OVER_STYLE: React.CSSProperties = { boxShadow: "0 0 0 2px #3b82f6" };

export function ElementRenderer({
  attributes,
  children,
  element,
  mode,
  roles,
}: ElementRendererProps) {
  const editor = useSlate();

  // Используем контекст для определения нахождения внутри inline-editor
  const isInsideInlineEditor = useInsideInlineEditor();

  // Make each element a potential drop target for inline editors, except for:
  // - inline editors themselves
  // - elements that are inline (like links and dice formulas)
  // - elements inside inline editors
  const isInlineEditor = element.type === "inline-editor";
  const isLinkElement = element.type === "link";
  const isDiceElement = element.type === "dice"; // inline chip, not a drop target
  const isTable = element.type === "table"; // new
  const isTableCell = element.type === "table-cell";
  const droppable =
    !isInlineEditor &&
    !isLinkElement &&
    !isDiceElement &&
    !isInsideInlineEditor;

  // Get the path of the current element in the editor
  let elementPath;
  let elementPathString: string | null = null;
  try {
    elementPath = ReactEditor.findPath(editor, element);
    elementPathString = JSON.stringify(elementPath); // Convert path to string for dnd-kit
  } catch (e) {
    // If we can't find the path, skip making this element droppable
    elementPath = null;
    elementPathString = null;
  }

  const { setNodeRef, isOver, active } =
    droppable && elementPathString
      ? useDroppable({
          id: elementPathString, // Using the element path as a string for the droppable ID
          data: {
            type: "element",
            element,
            path: elementPath,
          },
        })
      : { setNodeRef: undefined, isOver: false, active: undefined };

  const isDraggedOver =
    isOver && active?.data?.current?.type === "inline-editor";

  // Create a combined ref that includes both the droppable ref and the attributes ref
  const combinedRef = useCallback(
    (node: HTMLElement | null) => {
      if (setNodeRef) setNodeRef(node);
      // Also set the attributes ref if it exists
      if (attributes.ref) {
        if (typeof attributes.ref === "function") {
          attributes.ref(node);
        } else {
          // Use the generic approach to assign to ref without triggering deprecation warning
          const attrRef = attributes.ref;
          if (attrRef && typeof attrRef === "object" && "current" in attrRef) {
            (attrRef as React.RefObject<HTMLElement | null>).current = node;
          }
        }
      }
    },
    [setNodeRef, attributes.ref],
  );

  const mergedProps = useMemo(
    () => ({
      ...attributes,
      ref: combinedRef,
      style: isDraggedOver ? DRAG_OVER_STYLE : undefined,
    }),
    [attributes, combinedRef, isDraggedOver],
  );

  // Plain, non-interactive link for the live editor — clicking here places
  // the cursor, it shouldn't open a lore popup or navigate. Bypassing
  // registry.renderElement here (same as the inline-editor bypass below)
  // means linkDef.render() — which returns the fully interactive LoreLink
  // for lore-heading matches — only ever runs in the static preview/reader
  // path (RenderMarkdown), never inside the live Editable.
  if (isLinkElement) {
    const { isInternal } = classifyLink((element as any).url ?? "");
    return (
      <a
        {...attributes}
        href={(element as any).url}
        className={linkClassName(isInternal, false)}
      >
        {children}
      </a>
    );
  }

  // inline-editor custom render for editor
  if (isInlineEditor) {
    return (
      <div
        {...attributes}
        contentEditable={false}
        className="select-none relative"
        onMouseDown={(e) => e.stopPropagation()}
        onClick={(e) => e.stopPropagation()}
        onKeyDown={(e) => e.stopPropagation()}
        onBeforeInput={(e) => e.stopPropagation()}
      >
        <InlineEditorDndWrapper
          element={element}
          mode={mode}
          roles={roles || []}
        >
          {children}
        </InlineEditorDndWrapper>
      </div>
    );
  }

  if (isTable) {
    return (
      <TableView attributes={mergedProps} element={element} editor={editor}>
        {children}
      </TableView>
    );
  }

  if (isTableCell) {
    return (
      <TableCellView attributes={mergedProps} element={element} editor={editor}>
        {children}
      </TableCellView>
    );
  }

  const renderProps = {
    attributes: mergedProps,
    children,
    element,
    mode,
    roles,
  };

  const rendered = registry.renderElement(renderProps as any);

  // Fallback for any element type without a registered definition, so
  // unrecognized content still renders instead of disappearing.
  // (This is also where "dice" ends up — diceDef.render() handles it.)
  return (
    <>{rendered ?? <p {...(renderProps.attributes as any)}>{children}</p>}</>
  );
}
