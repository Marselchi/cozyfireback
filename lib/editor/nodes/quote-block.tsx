import type {
  BlockNodeDefinition,
  Token,
  ParserContext,
  ParseResult,
  SerializeHelpers,
} from "@/types/node-definition";
import type { Descendant } from "slate";
import quoteBlockPlugin from "../md-plugins/quote-block";

export const quoteBlockDef: BlockNodeDefinition = {
  type: "quote-block",
  kind: "block",
  mdPlugin: quoteBlockPlugin,

  match(token: Token): boolean {
    return token.type === "quote_block";
  },

  parse(ctx: ParserContext): ParseResult {
    const token = ctx.current()!;
    ctx.advance();
    return {
      type: "quote-block",
      children: [{ text: token.content }],
    };
  },

  serialize(node: Descendant, helpers: SerializeHelpers): string | null {
    if (
      typeof node !== "object" ||
      !("type" in node) ||
      (node as any).type !== "quote-block"
    ) {
      return null;
    }
    return `<? ${helpers.serializeChildren((node as any).children)}`;
  },

  render({ attributes, children }: any) {
    return (
      <blockquote
        {...attributes}
        className="my-2 border-l-4 border-primary pl-4 bg-primary/5 italic"
      >
        <span>{children}</span>
      </blockquote>
    );
  },
};
