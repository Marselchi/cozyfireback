import type {
  BlockNodeDefinition,
  Token,
  ParserContext,
  ParseResult,
  SerializeHelpers,
} from "@/types/node-definition";
import type { Descendant } from "slate";
import type {
  InlineEditorElement,
  InlineMetadata,
  SpoilerType,
} from "@/types/editor";
import restrictedBlockPlugin from "../md-plugins/restricted-block";

type RestrictedTokenMetadata = {
  metadata?: InlineMetadata;
};

// Kept in sync with SPOILER_STYLE in components/editor/inline-editor-block.tsx
// so the reader view (RenderMarkdown, static — never sees the live editor)
// shows the same visual distinction between "Обычный" and "Шанс" spoilers.
const SPOILER_CLASS: Record<SpoilerType, string> = {
  normal: "my-2 rounded border border-amber-400 p-2 bg-current/10",
  chance: "my-2 rounded border border-violet-400 p-2 bg-current/10",
};

export const inlineEditorDef: BlockNodeDefinition = {
  type: "inline-editor",
  kind: "block",
  priority: 100,
  mdPlugin: restrictedBlockPlugin,

  match(token: Token): boolean {
    return token.type === "restricted_block_open";
  },

  parse(ctx: ParserContext): ParseResult {
    const openToken = ctx.current()!;
    const restrictedMeta = openToken.meta as
      | RestrictedTokenMetadata
      | undefined;

    const metadata = restrictedMeta?.metadata;
    const realId = metadata?.id;

    const roles = metadata?.roles ?? [];

    const spoilerType: SpoilerType = metadata?.spoilerType ?? "normal";

    const chance =
      metadata?.spoilerType === "chance" ? metadata.chance : undefined;

    ctx.advance();
    let children = ctx.parseChildren(["restricted_block_close"]);
    if (ctx.current()?.type === "restricted_block_close") ctx.advance();

    // Split paragraphs by softbreak (notebook line semantics)
    children = splitParagraphsBySoftbreak(children);

    return {
      type: "inline-editor",
      id: `inline-${Date.now()}-${Math.random().toString(36).slice(2, 11)}`,
      realId,
      roles,
      spoilerType,
      chance,
      content: children,
      children: [{ text: "" }],
    };
  },

  serialize(node: Descendant, helpers: SerializeHelpers): string | null {
    if (
      typeof node !== "object" ||
      !("type" in node) ||
      (node as any).type !== "inline-editor"
    ) {
      return null;
    }
    const n = node as InlineEditorElement;

    // Reconstruct metadata from whatever fields live on the node besides
    // the structural ones — this mirrors parse()'s spread and keeps new
    // metadata fields working without touching serialize either.
    const {
      type: _type,
      id: _id,
      realId,
      roles,
      spoilerType,
      chance,
      content: _content,
      children: _children,
      ...rest
    } = n;

    const id = n.realId ? `id="${n.realId}"` : `localId="${n.id}"`;

    return `\n{restrictedBlock ${id}}`;
  },

  render({ attributes, children, element }: any) {
    const spoilerType: SpoilerType = element?.spoilerType ?? "normal";
    return (
      <div {...attributes} className={SPOILER_CLASS[spoilerType]}>
        {children}
      </div>
    );
  },
};

function serializeChildren(
  nodes: Descendant[],
  helpers: SerializeHelpers,
): string {
  return nodes
    .map((n) => helpers.serializeNode(n))
    .filter((s) => s !== null && s !== "")
    .join("\n");
}

function splitParagraphsBySoftbreak(nodes: Descendant[]): Descendant[] {
  const result: Descendant[] = [];

  for (const node of nodes) {
    if (
      typeof node === "object" &&
      "type" in node &&
      (node as any).type === "paragraph"
    ) {
      const split = splitParagraph(node as any);
      result.push(...split);
    } else {
      result.push(node);
    }
  }

  return result;
}

function splitParagraph(paragraph: any): any[] {
  const paragraphs: any[] = [];
  let current: any[] = [];

  for (const child of paragraph.children) {
    if ("text" in child && child.softbreak) {
      paragraphs.push({
        type: "paragraph",
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

  return paragraphs;
}
