// Ensure all node definitions are registered
import "./nodes/index";

import { Text, Element as SlateElement } from "slate";
import type { Descendant } from "slate";
import type { CustomText } from "@/types/editor";
import type { SerializeHelpers } from "@/types/node-definition";
import { registry } from "./registry";

function serializeText(node: CustomText): string {
  let text = node.text;
  if (text.length === 0) return text;

  // Preserve leading/trailing whitespace outside marks
  const parts = /^(\s*)([\s\S]*?)(\s*)$/.exec(text);
  if (!parts) return text;
  const [, leading, content, trailing] = parts;

  if (!content) return text;

  let marked = content;
  if (node.code) marked = `\`${marked}\``;
  if (node.strikethrough) marked = `~~${marked}~~`;
  if (node.underline) marked = `__${marked}__`;
  if (node.bold && node.italic) {
    marked = `***${marked}***`;
  } else if (node.bold) {
    marked = `**${marked}**`;
  } else if (node.italic) {
    marked = `_${marked}_`;
  }

  return `${leading}${marked}${trailing}`;
}

function makeHelpers(isPreview: boolean): SerializeHelpers {
  const helpers: SerializeHelpers = {
    isPreview,
    serializeNode(node: Descendant): string | null {
      return serializeNodeInternal(node, helpers);
    },
    serializeChildren(nodes: Descendant[]): string {
      return nodes
        .map((n) => serializeNodeInternal(n, helpers))
        .filter((s) => s !== null)
        .join("");
    },
  };
  return helpers;
}

function serializeNodeInternal(
  node: Descendant,
  helpers: SerializeHelpers,
): string | null {
  // Text leaf
  if (Text.isText(node)) {
    return serializeText(node as CustomText);
  }

  // Try registry (blocks + inline-like nodes such as link, image, inline-editor)
  const fromRegistry = registry.serialize(node, helpers);
  if (fromRegistry !== null) return fromRegistry;

  // Fallback: serialize children joined
  if (SlateElement.isElement(node) && "children" in node) {
    return helpers.serializeChildren((node as any).children);
  }

  return null;
}

/**
 * Serialize Slate nodes to markdown.
 * All blocks joined with single \n (notebook semantics).
 * Empty paragraphs become " " to preserve blank visual lines.
 */
export function serializeToMarkdown(nodes: Descendant[]): string {
  const helpers = makeHelpers(false);
  return nodes
    .map((n) => serializeNodeInternal(n, helpers))
    .filter((s) => s !== null)
    .join("\n");
}

/**
 * Preview serialization: inline-editor → restrictedBlock.
 */
export function serializeToMarkdownPreview(nodes: Descendant[]): string {
  const helpers = makeHelpers(true);
  return nodes
    .map((n) => serializeNodeInternal(n, helpers))
    .filter((s) => s !== null)
    .join("\n");
}
