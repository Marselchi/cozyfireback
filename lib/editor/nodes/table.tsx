import type {
  BlockNodeDefinition,
  Token,
  ParserContext,
  ParseResult,
  SerializeHelpers,
} from "@/types/node-definition";
import { Children } from "react";
import type { Descendant } from "slate";

type Align = "left" | "center" | "right" | null;

function extractAlign(token: Token): Align {
  const style = token.attrGet("style");
  if (style?.includes("right")) return "right";
  if (style?.includes("center")) return "center";
  if (style?.includes("left")) return "left";
  const align = token.attrGet("align"); // older markdown-it versions
  return align === "right" || align === "center" || align === "left"
    ? align
    : null;
}

function parseCell(ctx: ParserContext): Descendant {
  const openTok = ctx.current()!;
  const isHeader = openTok.type === "th_open";
  const align = extractAlign(openTok);
  const closeType = isHeader ? "th_close" : "td_close";
  ctx.advance(); // past th_open/td_open

  let children: Descendant[] = [{ text: "" } as any];
  const inlineToken = ctx.current();
  if (inlineToken?.type === "inline" && Array.isArray(inlineToken.children)) {
    children = ctx.parseInline(inlineToken.children) as any;
    ctx.advance(); // past the inline token
  }

  if (ctx.current()?.type === closeType) ctx.advance();

  return { type: "table-cell", header: isHeader, align, children } as any;
}

function parseRow(ctx: ParserContext): Descendant {
  ctx.advance(); // past tr_open
  const cells: Descendant[] = [];
  let header = false;

  while (ctx.current() && ctx.current()!.type !== "tr_close") {
    const t = ctx.current()!;
    if (t.type === "th_open" || t.type === "td_open") {
      if (t.type === "th_open") header = true;
      cells.push(parseCell(ctx));
    } else {
      ctx.advance(); // defensive: skip anything unexpected
    }
  }

  if (ctx.current()?.type === "tr_close") ctx.advance();
  return { type: "table-row", header, children: cells } as any;
}

export const tableDef: BlockNodeDefinition = {
  type: "table",
  kind: "block",
  priority: 10,

  match(token: Token): boolean {
    return token.type === "table_open";
  },

  // Manual, self-contained walk — deliberately does NOT delegate thead/tbody/tr
  // to separate registry-matched defs via nested parseChildren. That approach
  // relied on token.level bookkeeping lining up between two independent
  // recursive calls, and one edge case let it disagree, letting the tbody's
  // row escape back up to the root parser loop as a stray sibling node.
  parse(ctx: ParserContext): ParseResult {
    ctx.advance(); // past table_open
    const rows: Descendant[] = [];

    while (ctx.current() && ctx.current()!.type !== "table_close") {
      const t = ctx.current()!;
      if (
        t.type === "thead_open" ||
        t.type === "thead_close" ||
        t.type === "tbody_open" ||
        t.type === "tbody_close"
      ) {
        ctx.advance(); // transparent wrappers; header-ness is tracked per-row instead
        continue;
      }
      if (t.type === "tr_open") {
        rows.push(parseRow(ctx));
        continue;
      }
      ctx.advance(); // defensive fallback
    }

    if (ctx.current()?.type === "table_close") ctx.advance();
    return { type: "table", children: rows } as any;
  },

  serialize(node: Descendant, helpers: SerializeHelpers): string | null {
    if (
      typeof node !== "object" ||
      !("type" in node) ||
      (node as any).type !== "table"
    ) {
      return null;
    }

    const rows = (node as any).children ?? [];
    const result: string[] = [];

    for (const row of rows) {
      const rowStr = helpers.serializeNode(row);
      if (!rowStr) continue;

      result.push(rowStr);

      // В Markdown после строки заголовков обязательно должна идти
      // разделительная строка (separator row), определяющая выравнивание.
      if ((row as any).header) {
        const cells = (row as any).children ?? [];
        const separators = cells.map((cell: any) => {
          switch (cell.align) {
            case "left":
              return ":---";
            case "center":
              return ":---:";
            case "right":
              return "---:";
            default:
              return "---";
          }
        });
        result.push("| " + separators.join(" | ") + " |");
      }
    }

    return result.join("\n");
  },

  // Reader/static path only — the live editor renders TableView instead (see ElementRenderer).
  render({ attributes, children, element }: any) {
    const rowsData = (element as any).children ?? [];
    const childArray = Children.toArray(children);
    const headerChildren = childArray.filter((_, i) => rowsData[i]?.header);
    const bodyChildren = childArray.filter((_, i) => !rowsData[i]?.header);

    return (
      <table
        {...attributes}
        className="my-2 border-collapse border border-gray-300 table-auto"
      >
        {headerChildren.length > 0 && <thead>{headerChildren}</thead>}
        <tbody>{bodyChildren}</tbody>
      </table>
    );
  },
};

export const tableRowDef: BlockNodeDefinition = {
  type: "table-row",
  kind: "block",
  priority: 10,

  // Parsing is handled entirely inside tableDef.parse — these stay false/null
  // so they're never picked up by registry.parse's dispatch loop, but the
  // def still needs to exist so render()/serialize() are reachable by type.
  match(): boolean {
    return false;
  },
  parse(): ParseResult {
    return null;
  },

  serialize(node: Descendant, helpers: SerializeHelpers): string | null {
    if (
      typeof node !== "object" ||
      !("type" in node) ||
      (node as any).type !== "table-row"
    ) {
      return null;
    }
    const cells = (node as any).children.map(
      (c: Descendant) => helpers.serializeNode(c) ?? "",
    );
    return "| " + cells.join(" | ") + " |";
  },

  render({ attributes, children }: any) {
    return (
      <tr {...attributes} className="border-b min-w-20 border-gray-300">
        {children}
      </tr>
    );
  },
};

export const tableCellDef: BlockNodeDefinition = {
  type: "table-cell",
  kind: "block",
  priority: 10,

  match(): boolean {
    return false;
  },
  parse(): ParseResult {
    return null;
  },

  serialize(node: Descendant, helpers: SerializeHelpers): string | null {
    if (
      typeof node !== "object" ||
      !("type" in node) ||
      (node as any).type !== "table-cell"
    ) {
      return null;
    }
    return helpers.serializeChildren((node as any).children);
  },

  render({ attributes, children, element }: any) {
    const Tag = element.header ? "th" : "td";
    const alignClass =
      element.align === "right"
        ? "text-right"
        : element.align === "center"
          ? "text-center"
          : "text-left";
    return (
      <Tag
        {...attributes}
        className={`border border-input px-2 py-1 align-top ${alignClass} ${
          element.header ? "font-semibold bg-muted" : ""
        }`}
      >
        {children}
      </Tag>
    );
  },
};
