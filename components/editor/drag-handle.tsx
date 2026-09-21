import { GripVertical } from "lucide-react";
import { useDragContext } from "./inline-editor-dnd-wrapper";

export function DragHandle() {
  const { attributes, listeners, setNodeRef } = useDragContext();

  return (
    <div
      ref={setNodeRef}
      className="cursor-grab active:cursor-grabbing opacity-100 transition-opacity"
      {...listeners}
      {...attributes}
      data-drag-handle
    >
      <GripVertical className="h-4 w-4 text-muted-foreground hover:text-foreground" />
    </div>
  );
}