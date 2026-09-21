import type {
  BlockNodeDefinition,
  Token,
  ParserContext,
  ParseResult,
  SerializeHelpers,
} from "@/types/node-definition";
import type { Descendant } from "slate";

export const paragraphDef: BlockNodeDefinition = {
  type: "paragraph",
  kind: "block",
  priority: 10,

  match(token: Token): boolean {
    return token.type === "paragraph_open";
  },

  parse(ctx: ParserContext): ParseResult {
    const openToken = ctx.current();
    if (openToken?.type !== "paragraph_open") return null;

    ctx.advance(); // skip _open

    const inlineTokens: Token[] = [];
    while (ctx.current()?.type === "inline") {
      const inline = ctx.current()!;
      inlineTokens.push(...(inline.children || []));
      ctx.advance();
    }

    if (ctx.current()?.type === "paragraph_close") {
      ctx.advance();
    }

    const children = ctx.parseInline(inlineTokens);

    // Split on softbreak to preserve notebook line-per-line semantics
    return splitParagraphsBySoftbreak({ type: "paragraph", children });
  },

  serialize(node: Descendant, helpers: SerializeHelpers): string | null {
    if (
      typeof node !== "object" ||
      !("type" in node) ||
      (node as any).type !== "paragraph"
    ) {
      return null;
    }
    const children = helpers.serializeChildren(node.children);
    const suffix = (node as any).endsWithHardBreak ? "  " : "";
    return (children === "" ? " " : children) + suffix;
  },

  render({ attributes, children, element }) {
    
    return (
      <div {...attributes} className="my-1 p">
        {children}
      </div>
    );
  },
};

function splitParagraphsBySoftbreak(paragraph: any): any[] {
  const paragraphs: any[] = [];
  let current: any[] = [];

  for (const child of paragraph.children) {
    if ("text" in child && child.softbreak) {
      const isHard = child.hardbreak === true;
      paragraphs.push({
        type: "paragraph",
        endsWithHardBreak: isHard,
        children: current.length > 0 ? current : [{ text: "" }],
      });
      current = [];
    } else {
      current.push(child);
    }
  }
  paragraphs.push({
    type: "paragraph",
    children: current.length > 0 ? current : [{ text: "" }],
  });

  return paragraphs.length === 1 ? paragraphs[0] : paragraphs;
}
