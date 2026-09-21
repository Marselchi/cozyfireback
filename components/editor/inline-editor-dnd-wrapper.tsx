import { createContext, useContext, ReactNode, useMemo } from "react";
import { useDraggable } from "@dnd-kit/core";
import { InlineEditorBlock } from "./inline-editor-block";
import type { InlineEditorElement, EditorMode } from "@/types/editor";
import { Role } from "@/types/editor-layout";

interface DragContextType {
  attributes: any;
  listeners: any;
  setNodeRef: (element: HTMLElement | null) => void;
}

const DragContext = createContext<DragContextType | null>(null);

export const useDragContext = () => {
  const context = useContext(DragContext);
  if (!context) {
    throw new Error("useDragContext must be used within a DragProvider");
  }
  return context;
};

// Контекст для блокировки droppable внутри inline-editor
export const InsideInlineEditorContext = createContext(false);

export const useInsideInlineEditor = () =>
  useContext(InsideInlineEditorContext);

type InlineEditorDndWrapperProps = {
  element: InlineEditorElement;
  mode: EditorMode;
  roles: Role[];
  children: ReactNode;
};

export function InlineEditorDndWrapper({
  element,
  mode,
  roles,
  children,
}: InlineEditorDndWrapperProps) {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: element.id ?? "none",
    data: {
      type: "inline-editor",
      element,
    },
  });

  const style = isDragging ? { opacity: 0.5 } : undefined;

  const contextValue = useMemo(
    () => ({
      attributes,
      listeners,
      setNodeRef,
    }),
    [attributes, listeners, setNodeRef],
  );

  return (
    <InsideInlineEditorContext.Provider value={true}>
      <DragContext.Provider value={contextValue}>
        <div
          style={style}
          className="relative"
          onMouseDown={(e) => {
            if (!(e.target as HTMLElement).closest("[data-drag-handle]")) {
              e.stopPropagation();
            }
          }}
        >
          <InlineEditorBlock element={element} mode={mode} roles={roles}>
            {children}
          </InlineEditorBlock>
        </div>
      </DragContext.Provider>
    </InsideInlineEditorContext.Provider>
  );
}
