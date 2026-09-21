// Ensure all node definitions are registered before parsing
import "./nodes/index";

import type { Descendant } from "slate";
import { registry } from "./registry";
import { ParserState } from "./parser-state";
import { getMarkdownIt } from "./tokenizer";

/**
 * Parse a markdown string into Slate Descendant[].
 * Uses markdown-it for tokenization, then dispatches via NodeRegistry.
 *
 * Notebook semantics: every source line becomes one Slate paragraph.
 * Softbreaks inside paragraphs are split into separate paragraph nodes.
 * Empty markdown lines become `{ type: "paragraph", children: [{ text: "" }] }`.
 */
export function parseMarkdownToSlate(markdown: string): Descendant[] {
  if (!markdown) {
    return [{ type: "paragraph", children: [{ text: "" }] }];
  }

  const md = getMarkdownIt(registry);
  const tokens = md.parse(markdown, {}) as any;

  const ctx = new ParserState(tokens, registry);
  const nodes: Descendant[] = [];
  let lastEndLine = 0;

  while (ctx.index < tokens.length) {
    const token = ctx.current();
    if (!token) break;

    // Skip stray closing tokens at root level
    if (token.type.endsWith("_close") && token.nesting === -1) {
      ctx.advance();
      continue;
    }

    if (Array.isArray(token.map)) {
      const blankLines = token.map[0] - lastEndLine;
      for (let i = 0; i < blankLines; i++) {
        nodes.push({ type: "paragraph", children: [{ text: "" }] });
      }
    }

    const result = registry.parse(token, ctx);

    if (Array.isArray(token.map)) {
      lastEndLine = token.map[1]; // ← update after parsing
    }

    if (Array.isArray(result)) {
      nodes.push(...result);
    } else if (result) {
      nodes.push(result);
    }
  }

  return nodes.length > 0
    ? nodes
    : [{ type: "paragraph", children: [{ text: "" }] }];
}
