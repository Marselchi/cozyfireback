import type {
  BlockNodeDefinition,
  Token,
  ParserContext,
  ParseResult,
  SerializeHelpers,
} from "@/types/node-definition";
import type { Descendant } from "slate";
import rollableBlockPlugin from "../md-plugins/rollable-block";
import { RollableNode } from "../renders/rollable";

type RollableTokenMetadata = {
  rollId?: number;
};

// Structural id is only for React/Slate keys — rollId is the number that
// tells <DiceRoll/> and the fetch what to roll/fetch for.
export type RollableElement = {
  type: "rollable";
  id: string;
  rollId: number;
  children: Descendant[];
};

export const rollableEditorDef: BlockNodeDefinition = {
  type: "rollable",
  kind: "block",
  priority: 100,
  mdPlugin: rollableBlockPlugin,

  match(token: Token): boolean {
    return token.type === "rollable_block";
  },

  parse(ctx: ParserContext): ParseResult {
    const token = ctx.current()!;
    const meta = token.meta as RollableTokenMetadata | undefined;
    const rollId = meta?.rollId;

    ctx.advance();

    return {
      type: "rollable",
      id: `rollable-${rollId ?? "unknown"}`,
      rollId,
      children: [{ text: "" }],
    };
  },

  serialize(node: Descendant, _helpers: SerializeHelpers): string | null {
    if (
      typeof node !== "object" ||
      !("type" in node) ||
      (node as any).type !== "rollable"
    ) {
      return null;
    }
    const n = node as RollableElement;
    return `\n{rollable id=${n.rollId}}`;
  },

  render({ element }: any) {
    const n = element as RollableElement;
    return <RollableNode rollId={n.rollId} />;
  },
};
