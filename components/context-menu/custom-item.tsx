import type {
  MenuCustom,
  ContextMenuRuntimeContext,
} from "@/lib/context-menu/types";

type Props = {
  item: MenuCustom;
  ctx: ContextMenuRuntimeContext;
};

/**
 * CustomItem is a pure render-prop delegate.
 * All rendering and business logic lives in the registry definition.
 */
export function CustomItem({ item, ctx }: Readonly<Props>) {
  return <>{item.render(ctx)}</>;
}
