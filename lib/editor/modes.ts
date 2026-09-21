import { Element as SlateElement } from "slate";
import type { Descendant } from "slate";
import { serializeToMarkdown } from "./serialize";
import { parseMarkdownToSlate } from "./parse";

const EMPTY_CONTENT: Descendant[] = [
  { type: "paragraph" as const, children: [{ text: "" }] },
];

/**
 * Convert parsed Slate tree → raw (one paragraph per markdown line).
 * inline-editor nodes are kept as-is.
 */
export function convertParsedToRaw(nodes: Descendant[]): Descendant[] {
  const result: Descendant[] = [];

  for (const node of nodes) {
    if (SlateElement.isElement(node) && node.type === "inline-editor") {
      result.push(node);
    } else {
      const markdown = serializeToMarkdown([node]);
      if (markdown) {
        const lines = markdown.split("\n");
        for (const line of lines) {
          result.push({
            type: "paragraph" as const,
            children: [{ text: line }],
          });
        }
      }
    }
  }

  return result.length > 0 ? result : EMPTY_CONTENT;
}

/**
 * Convert raw (line-per-paragraph) → parsed Slate tree.
 * inline-editor nodes are kept in place; surrounding text lines are
 * joined and re-parsed with markdown-it.
 */
export function convertRawToParsed(nodes: Descendant[]): Descendant[] {
  const result: Descendant[] = [];
  const textLines: string[] = [];

  const flushTextLines = () => {
    if (textLines.length === 0) return;
    const parsed = parseMarkdownToSlate(textLines.join("\n"));
    result.push(...parsed);
    textLines.length = 0;
  };

  for (const node of nodes) {
    if (SlateElement.isElement(node) && node.type === "inline-editor") {
      flushTextLines();
      result.push(node);
    } else if (SlateElement.isElement(node) && "children" in node) {
      const text = (node.children as any[]).map((c) => c.text || "").join("");
      textLines.push(text);
    }
  }

  flushTextLines();

  return result.length > 0 ? result : EMPTY_CONTENT;
}
