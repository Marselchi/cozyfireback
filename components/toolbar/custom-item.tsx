import type {
  CustomItem as CustomItemDef,
  ToolbarRuntimeContext,
} from "@/lib/toolbar/types";

type Props = {
  item: CustomItemDef;
  ctx: ToolbarRuntimeContext;
};

/**
 * CustomItem is a pure render-prop delegate.
 * All rendering and business logic lives in the registry definition.
 */
export function CustomItem({ item, ctx }: Readonly<Props>) {
  return <>{item.render(ctx)}</>;
}
