import type { BlockNodeDefinition, Token, ParserContext, ParseResult, SerializeHelpers } from "@/types/node-definition";
import type { Descendant } from "slate";
import { Element } from "slate";
import type { ListItemElement } from "@/types/editor";

export const bulletedListDef: BlockNodeDefinition = {
  type: "bulleted-list",
  kind: "block",

  match(token: Token): boolean {
    return token.type === "bullet_list_open";
  },

  parse(ctx: ParserContext): ParseResult {
    ctx.advance();
    const items = ctx.parseChildren(["bullet_list_close"]) as ListItemElement[];
    if (ctx.current()?.type === "bullet_list_close") ctx.advance();
    return { type: "bulleted-list", children: items };
  },

  serialize(node: Descendant, helpers: SerializeHelpers): string | null {
    if (typeof node !== "object" || !("type" in node) || (node as any).type !== "bulleted-list") {
      return null;
    }
    return (node as any).children
      .map((item: Descendant) => helpers.serializeNode(item))
      .filter(Boolean)
      .join("\n");
  },

  render({ attributes, children }: any) {
    return (
      <ul {...attributes} className="my-2 list-disc pl-6">
        {children}
      </ul>
    );
  },
};

export const numberedListDef: BlockNodeDefinition = {
  type: "numbered-list",
  kind: "block",

  match(token: Token): boolean {
    return token.type === "ordered_list_open";
  },

  parse(ctx: ParserContext): ParseResult {
    ctx.advance();
    const items = ctx.parseChildren(["ordered_list_close"]) as ListItemElement[];
    if (ctx.current()?.type === "ordered_list_close") ctx.advance();
    return { type: "numbered-list", children: items };
  },

  serialize(node: Descendant, helpers: SerializeHelpers): string | null {
    if (typeof node !== "object" || !("type" in node) || (node as any).type !== "numbered-list") {
      return null;
    }
    return (node as any).children
      .map((item: Descendant, idx: number) => {
        const itemText = helpers.serializeNode(item);
        if (itemText?.startsWith("- ")) return `${idx + 1}. ${itemText.slice(2)}`;
        return `${idx + 1}. ${itemText || ""}`;
      })
      .join("\n");
  },

  render({ attributes, children }: any) {
    return (
      <ol {...attributes} className="my-2 list-decimal pl-6">
        {children}
      </ol>
    );
  },
};

export const listItemDef: BlockNodeDefinition = {
  type: "list-item",
  kind: "block",

  match(token: Token): boolean {
    return token.type === "list_item_open";
  },

  parse(ctx: ParserContext): ParseResult {
    ctx.advance();
    let children = ctx.parseChildren(["list_item_close"]);
    if (ctx.current()?.type === "list_item_close") ctx.advance();

    // Unwrap single paragraph
    if (children.length === 1) {
      const first = children[0];
      if (Element.isElement(first) && first.type === "paragraph") {
        children = first.children as any;
      }
    }

    return { type: "list-item", children: children as any };
  },

  serialize(node: Descendant, helpers: SerializeHelpers): string | null {
    if (typeof node !== "object" || !("type" in node) || (node as any).type !== "list-item") {
      return null;
    }
    return `- ${helpers.serializeChildren((node as any).children)}`;
  },

  render({ attributes, children }: any) {
    return <li {...attributes}>{children}</li>;
  },
};
