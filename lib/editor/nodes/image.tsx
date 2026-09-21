import type { BlockNodeDefinition, Token, ParserContext, ParseResult, SerializeHelpers } from "@/types/node-definition";
import type { Descendant } from "slate";
import type { ImageElement } from "@/types/editor";

export const imageDef: BlockNodeDefinition = {
  type: "image",
  kind: "block",

  match(_token: Token): boolean {
    return false; // Images handled inline by parseInlineTokens
  },

  parse(_ctx: ParserContext): ParseResult {
    return null;
  },

  serialize(node: Descendant, _helpers: SerializeHelpers): string | null {
    if (typeof node !== "object" || !("type" in node) || (node as any).type !== "image") {
      return null;
    }
    const n = node as ImageElement;
    const title = n.title ? ` "${n.title}"` : "";
    return `![${n.alt}](${n.src}${title})`;
  },

  render({ attributes, element }: any) {
    return (
      <img
        {...attributes}
        src={element.src}
        alt={element.alt}
        title={element.title}
        className="max-w-full rounded"
      />
    );
  },
};
