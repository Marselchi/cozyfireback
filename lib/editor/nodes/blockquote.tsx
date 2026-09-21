import type {
  BlockNodeDefinition,
  Token,
  ParserContext,
  ParseResult,
  SerializeHelpers,
} from "@/types/node-definition";
import type { Descendant } from "slate";

export const blockquoteDef: BlockNodeDefinition = {
  type: "blockquote",
  kind: "block",

  match(token: Token): boolean {
    return token.type === "blockquote_open";
  },

  parse(ctx: ParserContext): ParseResult {
    ctx.advance();
    const children = ctx.parseChildren(["blockquote_close"]);
    if (ctx.current()?.type === "blockquote_close") ctx.advance();
    return { type: "blockquote", children: children as any };
  },

  serialize(node: Descendant, helpers: SerializeHelpers): string | null {
    if (
      typeof node !== "object" ||
      !("type" in node) ||
      (node as any).type !== "blockquote"
    ) {
      return null;
    }
    return `> ${helpers.serializeChildren((node as any).children)}`;
  },

  render({ attributes, children }: any) {
    return (
      <blockquote
        {...attributes}
        className="my-2 border-l-4 border-primary pl-4 italic text-muted-foreground"
      >
        {children}
      </blockquote>
    );
  },
};
