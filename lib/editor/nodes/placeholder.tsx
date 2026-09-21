import type { BlockNodeDefinition, Token, ParserContext, ParseResult, SerializeHelpers } from "@/types/node-definition";
import type { Descendant } from "slate";
import placeholderPlugin from "../md-plugins/placeholder";

// Placeholder is an inline token handled by parseInlineTokens.
// This def only provides the mdPlugin + serialize.
export const placeholderDef: BlockNodeDefinition = {
  type: "placeholder",
  kind: "block",
  mdPlugin: placeholderPlugin,

  match(_token: Token): boolean {
    return false;
  },

  parse(_ctx: ParserContext): ParseResult {
    return null;
  },

  // Placeholders appear as CustomText nodes with placeholder=true inside paragraph children.
  // Serialization is handled by the text serializer in serialize.ts.
  serialize(_node: Descendant, _helpers: SerializeHelpers): string | null {
    return null;
  },

  render({ attributes, children }: any) {
    return <span {...attributes}>{children}</span>;
  },
};
