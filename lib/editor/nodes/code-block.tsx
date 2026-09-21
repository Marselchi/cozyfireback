import type { BlockNodeDefinition, Token, ParserContext, ParseResult, SerializeHelpers } from "@/types/node-definition";
import type { Descendant } from "slate";

export const codeBlockDef: BlockNodeDefinition = {
  type: "code-block",
  kind: "block",

  match(token: Token): boolean {
    return token.type === "code_block" || token.type === "fence";
  },

  parse(ctx: ParserContext): ParseResult {
    const token = ctx.current()!;
    ctx.advance();
    return {
      type: "code-block",
      children: [{ text: token.content.replace(/\n$/, "") }],
    };
  },

  serialize(node: Descendant, helpers: SerializeHelpers): string | null {
    if (typeof node !== "object" || !("type" in node) || (node as any).type !== "code-block") {
      return null;
    }
    const content = helpers.serializeChildren((node as any).children);
    return `\`\`\`\n${content}\n\`\`\``;
  },

  render({ attributes, children }: any) {
    return (
      <pre
        {...attributes}
        className="my-2 overflow-x-auto rounded-md bg-muted p-4 font-mono text-sm"
      >
        <code>{children}</code>
      </pre>
    );
  },
};
