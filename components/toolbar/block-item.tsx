import type React from "react";
import { Toggle } from "@/components/ui/toggle";
import { isBlockActive, toggleBlock } from "@/lib/editor-utils";
import type {
  BlockItem as BlockItemDef,
  ToolbarRuntimeContext,
} from "@/lib/toolbar/types";

type Props = {
  item: BlockItemDef;
  ctx: ToolbarRuntimeContext;
};

const stopMouseDown = (e: React.MouseEvent) => {
  e.preventDefault();
  e.stopPropagation();
};

export function BlockItem({ item, ctx }: Props) {
  const isRawMode = ctx.mode === "raw";
  const isActive = !isRawMode && isBlockActive(ctx.editor, item.format);
  const Icon = item.icon;

  return (
    <Toggle
      size="sm"
      pressed={isActive}
      onPressedChange={() => toggleBlock(ctx.editor, item.format)}
      aria-label={item.label}
      disabled={isRawMode}
      className={isRawMode ? "opacity-50 cursor-not-allowed" : ""}
      onMouseDown={stopMouseDown}
    >
      <Icon className="h-3 w-3" />
    </Toggle>
  );
}
