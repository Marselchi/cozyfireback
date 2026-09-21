import type React from "react";
import { Toggle } from "@/components/ui/toggle";
import { isMarkActive, toggleMark } from "@/lib/editor-utils";
import type {
  MarkItem as MarkItemDef,
  ToolbarRuntimeContext,
} from "@/lib/toolbar/types";

type Props = {
  item: MarkItemDef;
  ctx: ToolbarRuntimeContext;
};

const stopMouseDown = (e: React.MouseEvent) => {
  e.preventDefault();
  e.stopPropagation();
};

export function MarkItem({ item, ctx }: Props) {
  const isRawMode = ctx.mode === "raw";
  const isActive = !isRawMode && isMarkActive(ctx.editor, item.format);
  const Icon = item.icon;

  return (
    <Toggle
      size="sm"
      pressed={isActive}
      onPressedChange={() => toggleMark(ctx.editor, item.format)}
      aria-label={item.label}
      disabled={isRawMode}
      className={isRawMode ? "opacity-50 cursor-not-allowed" : ""}
      onMouseDown={stopMouseDown}
    >
      <Icon className="h-3 w-3" />
    </Toggle>
  );
}
