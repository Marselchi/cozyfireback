import type { ComponentType } from "react";
import { NormalBlockPanel } from "@/components/blocks/normal-block-panel";
import { ChanceBlockPanel } from "@/components/blocks/chance-block-panel";
import type { BlockDetail, BlockType } from "@/types/blocks";

export interface BlockTypeComponentProps {
  block: BlockDetail;
}

export const blockTypeRegistry: Record<
  BlockType,
  ComponentType<BlockTypeComponentProps>
> = {
  normal: NormalBlockPanel,
  chance: ChanceBlockPanel,
};

export function registerBlockType(
  type: BlockType,
  component: ComponentType<BlockTypeComponentProps>,
) {
  blockTypeRegistry[type] = component;
}
