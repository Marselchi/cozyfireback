import type {
  InlineNodeDefinition,
  Token,
  ParserContext,
  PossibleInlineChildren,
  SerializeHelpers,
} from "@/types/node-definition";
import type { Descendant } from "slate";
import type { DiceElement } from "@/types/editor";
import diceInlinePlugin from "../md-plugins/dice-inline";
import { DiceFormulaNode } from "../renders/dice-formula";

function formatModifier(modifier: number): string {
  if (modifier === 0) return "";
  return modifier > 0 ? `+${modifier}` : `${modifier}`;
}

function makeDiceElement(token: Token): DiceElement {
  const meta = token.meta as {
    count?: number;
    sides?: number;
    modifier?: number;
    letter?: string;
  } | null;
  return {
    type: "dice",
    id: `dice-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`,
    count: meta?.count ?? 1,
    sides: meta?.sides ?? 6,
    modifier: meta?.modifier ?? 0,
    letter: meta?.letter === "к" ? "к" : "d",
    children: [{ text: "" }],
  };
}

export const diceDef: InlineNodeDefinition = {
  type: "dice",
  kind: "inline",
  mdPlugin: diceInlinePlugin,

  // NOTE: parseInlineTokens (lib/editor/inline.ts) is a hand-written switch
  // over token.type, not a registry dispatch — the actual token -> Slate
  // conversion for "dice" tokens happens there (see the "dice" case), using
  // the same shape as makeDiceElement above. matchInline/parseInline exist
  // only to satisfy the InlineNodeDefinition shape / keep this def usable
  // if that switch is ever made registry-driven.
  matchInline(token: Token): boolean {
    return token.type === "dice";
  },

  parseInline(token: Token, _ctx: ParserContext): PossibleInlineChildren {
    return makeDiceElement(token);
  },

  serialize(node: Descendant, _helpers: SerializeHelpers): string | null {
    if (
      typeof node !== "object" ||
      !("type" in node) ||
      (node as any).type !== "dice"
    ) {
      return null;
    }
    const n = node as DiceElement;
    const letter = n.letter === "к" ? "к" : "d";
    return `{${n.count}${letter}${n.sides}${formatModifier(n.modifier)}}`;
  },

  render({ attributes, children, element }: any) {
    const n = element as DiceElement;
    return (
      <span
        {...attributes}
        contentEditable={false}
        className="inline-flex align-middle"
      >
        <DiceFormulaNode
          count={n.count}
          sides={n.sides}
          modifier={n.modifier}
        />
        <span className="hidden">{children}</span>
      </span>
    );
  },
};
