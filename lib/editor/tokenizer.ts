import MarkdownIt from "markdown-it";
import type { NodeRegistry } from "./registry";

let _md: MarkdownIt | null = null;

export function getMarkdownIt(registry: NodeRegistry): MarkdownIt {
  if (_md) return _md;

  _md = new MarkdownIt({
    html: false,
    linkify: false,
    typographer: false,
  });

  _md.enable([
    "heading",
    "paragraph",
    "list",
    "blockquote",
    "code",
    "image",
    "link",
  ]);
  _md.use(require("markdown-it-mark"));

  // Apply all plugins registered by node definitions
  registry.applyPlugins(_md);

  return _md;
}

/** Reset cached instance (useful in tests) */
export function resetMarkdownIt(): void {
  _md = null;
}
