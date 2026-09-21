import { ContextMenuItem } from "@/components/ui/context-menu";
import { cn } from "@/lib/utils";
import type {
  MenuAction,
  ContextMenuRuntimeContext,
} from "@/lib/context-menu/types";

type Props = {
  item: MenuAction;
  ctx: ContextMenuRuntimeContext;
};

export function ActionItem({ item, ctx }: Readonly<Props>) {
  const Icon = item.icon;
  const disabled = item.disabled?.(ctx) ?? false;

  return (
    <ContextMenuItem
      onClick={() => item.onSelect(ctx)}
      disabled={disabled}
      className={cn(
        "gap-2",
        item.variant === "destructive" &&
          "text-destructive focus:text-destructive",
      )}
    >
      <Icon className="h-4 w-4" />
      {item.label}
    </ContextMenuItem>
  );
}
