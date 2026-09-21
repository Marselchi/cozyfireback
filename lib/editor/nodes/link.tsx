import type {
  BlockNodeDefinition,
  Token,
  ParserContext,
  ParseResult,
  SerializeHelpers,
} from "@/types/node-definition";
import type { Descendant } from "slate";
import type { LinkElement } from "@/types/editor";
import CustomLink from "@/components/no-prefetch-link";
import { classifyLink, linkClassName } from "../util/link-util";
import { LoreLink } from "@/components/lore/lore-link";

// Link is handled inline, but needs a block def for serialize + render
export const linkDef: BlockNodeDefinition = {
  type: "link",
  kind: "block",

  match(_token: Token): boolean {
    return false; // Links are handled inline by parseInlineTokens
  },

  parse(_ctx: ParserContext): ParseResult {
    return null;
  },

  serialize(node: Descendant, helpers: SerializeHelpers): string | null {
    if (
      typeof node !== "object" ||
      !("type" in node) ||
      (node as any).type !== "link"
    ) {
      return null;
    }
    const n = node as LinkElement;
    const text = helpers.serializeChildren(n.children);
    return `[${text}](${n.url})`;
  },

  render({ attributes, element, children }: any) {
    const { isInternal, lore, href } = classifyLink(element.url);
    const className = linkClassName(isInternal, lore !== undefined);

    if (lore) {
      return (
        <LoreLink
          attributes={attributes}
          href={element.url}
          className={`${className} lore-link`}
          lore={lore}
        >
          {children}
        </LoreLink>
      );
    }

    // For internal links, always navigate via the normalized *relative*
    // href (never the raw absolute element.url). next/link only performs
    // client-side routing for an absolute URL when it exactly matches the
    // current origin — a stored link with/without "www." (or built from a
    // stale NEXT_PUBLIC_DOMAIN) can mismatch that check and silently fall
    // back to a full page reload, even though classifyLink correctly
    // flagged it as internal for styling purposes. A relative href sidesteps
    // that entirely.
    const finalHref = isInternal && href ? href : element.url;

    return (
      <CustomLink {...attributes} href={finalHref} className={className}>
        {children}
      </CustomLink>
    );
  },
};
