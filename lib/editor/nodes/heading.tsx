import type {
  BlockNodeDefinition,
  Token,
  ParserContext,
  ParseResult,
  SerializeHelpers,
} from "@/types/node-definition";
import type { Descendant } from "slate";
import type { HeadingElement } from "@/types/editor";
import type { ElementType } from "react";
import { Node } from "slate";
const headingClasses: Record<1 | 2 | 3 | 4 | 5 | 6, string> = {
  1: "text-4xl font-bold my-2",
  2: "text-3xl font-bold my-2",
  3: "text-2xl font-semibold my-2",
  4: "text-xl font-semibold my-2",
  5: "text-lg font-medium my-2",
  6: "text-base font-medium my-2",
};

function slugify(text: string): string {
  return text
    .toLowerCase()
    .trim()
    .replace(/[^\p{L}\p{N}]+/gu, "-")
    .replace(/^-|-$/g, "");
}
export const headingDef: BlockNodeDefinition = {
  type: "heading",
  kind: "block",
  priority: 20,

  match(token: Token): boolean {
    return token.type === "heading_open";
  },

  parse(ctx: ParserContext): ParseResult {
    const openToken = ctx.current()!;
    const level = parseInt(
      openToken.tag.slice(1),
      10,
    ) as HeadingElement["level"];

    ctx.advance();

    const inlineTokens: Token[] = [];
    while (ctx.current()?.type === "inline") {
      const inline = ctx.current()!;
      inlineTokens.push(...(inline.children || []));
      ctx.advance();
    }

    if (ctx.current()?.type === "heading_close") {
      ctx.advance();
    }

    return {
      type: "heading",
      level,
      children: ctx.parseInline(inlineTokens),
    };
  },

  serialize(node: Descendant, helpers: SerializeHelpers): string | null {
    if (
      typeof node !== "object" ||
      !("type" in node) ||
      (node as any).type !== "heading"
    ) {
      return null;
    }
    const n = node as HeadingElement;
    return `${"#".repeat(n.level)} ${helpers.serializeChildren(n.children)}`;
  },

  render({ attributes, children, element }: any) {
    const level = (element.level ?? 2) as 1 | 2 | 3 | 4 | 5 | 6;
    const Tag = `h${level}` as ElementType;
    const text = Node.string({ children: element.children } as any);
    const id = slugify(text);
    return (
      <Tag
        {...attributes}
        id={id || undefined}
        className={headingClasses[level]}
      >
        {children}
      </Tag>
    );
  },
};
