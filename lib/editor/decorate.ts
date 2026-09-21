// Ensure all node definitions are registered
import "./nodes/index";

import type { NodeEntry } from "slate";
import { Element as SlateElement } from "slate";
import type { CustomRange } from "@/types/editor";
import { registry } from "./registry";

/**
 * Decorates raw-mode text nodes with syntax highlighting ranges.
 * Runs all MarkDefinition.decorate() and InlineNodeDefinition.decorate() fns.
 * inline-editor void nodes are skipped.
 */
export function decorateRaw([node, path]: NodeEntry): CustomRange[] {
  if (SlateElement.isElement(node) && node.type === "inline-editor") {
    return [];
  }
  return registry.decorate([node, path]);
}

/**
 * Alias kept for compatibility with existing imports.
 */
export { decorateRaw as decorateMarkdown };
