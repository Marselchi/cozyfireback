import type React from "react";
import { useState } from "react";
import { Transforms, Editor, Element } from "slate";

import { Toggle } from "@/components/ui/toggle";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

import { isBlockActive, toggleBlock } from "@/lib/editor-utils";
import type {
  ExpandableItem as ExpandableItemDef,
  ExpandableChild,
  ToolbarRuntimeContext,
} from "@/lib/toolbar/types";

type Props = {
  item: ExpandableItemDef;
  ctx: ToolbarRuntimeContext;
};

const stopMouseDown = (e: React.MouseEvent) => {
  e.preventDefault();
  e.stopPropagation();
};

/**
 * Activates a heading child by ensuring the block type is "heading" and then
 * setting the level on the node.  Non-heading children just call toggleBlock.
 */
function activateChild(
  editor: ToolbarRuntimeContext["editor"],
  child: ExpandableChild,
) {
  if (child.format === "heading" && child.level !== undefined) {
    if (isChildActive(editor, child)) {
      toggleBlock(editor, "heading");
    } else {
      Transforms.setNodes(editor, {
        type: "heading",
        level: child.level,
      } as Partial<Parameters<typeof Transforms.setNodes>[1]>);
    }
  } else {
    toggleBlock(editor, child.format);
  }
}

/**
 * Returns true when the given child is the currently active block.
 * For headings we also match on level.
 */
function isChildActive(
  editor: ToolbarRuntimeContext["editor"],
  child: ExpandableChild,
): boolean {
  if (!isBlockActive(editor, child.format)) return false;
  if (child.format === "heading" && child.level !== undefined) {
    // Check the level of the currently selected heading node.
    const [match] = Array.from(
      editor.nodes({
        match: (n) =>
          !Editor.isEditor(n) &&
          Element.isElement(n) &&
          (n as { type: string }).type === "heading" &&
          (n as { level?: number }).level === child.level,
      }),
    );
    return !!match;
  }
  return true;
}

export function ExpandableItem({ item, ctx }: Props) {
  const [open, setOpen] = useState(false);
  const isRawMode = ctx.mode === "raw";
  const TriggerIcon = item.icon;

  // The trigger appears "active" when any of its children is the current block.
  const anyChildActive =
    !isRawMode &&
    item.children.some((child) => isChildActive(ctx.editor, child));

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Toggle
          size="sm"
          pressed={anyChildActive}
          aria-label={item.label}
          disabled={isRawMode}
          className={isRawMode ? "opacity-50 cursor-not-allowed" : ""}
          onMouseDown={stopMouseDown}
        >
          <TriggerIcon className="h-3 w-3" />
        </Toggle>
      </PopoverTrigger>

      <PopoverContent
        className="flex w-auto flex-col gap-0.5 p-1"
        onMouseDown={stopMouseDown}
      >
        {item.children.map((child) => {
          const ChildIcon = child.icon;
          const active = isChildActive(ctx.editor, child);

          return (
            <Toggle
              key={child.id}
              size="sm"
              pressed={active}
              onPressedChange={() => {
                activateChild(ctx.editor, child);
                setOpen(false);
              }}
              aria-label={child.label}
              className="justify-start gap-2 px-2"
              onMouseDown={stopMouseDown}
            >
              {ChildIcon && <ChildIcon className="h-3 w-3" />}
              <span className="text-xs">{child.label}</span>
            </Toggle>
          );
        })}
      </PopoverContent>
    </Popover>
  );
}
