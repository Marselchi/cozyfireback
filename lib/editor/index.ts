// Public barrel — import from "@/lib/editor"
export { parseMarkdownToSlate } from "./parse";
export { serializeToMarkdown, serializeToMarkdownPreview } from "./serialize";
export { convertParsedToRaw, convertRawToParsed } from "./modes";
export { decorateRaw, decorateMarkdown } from "./decorate";
export { registry } from "./registry";

// Re-export types
export type { NodeDefinition, BlockNodeDefinition, InlineNodeDefinition, MarkDefinition } from "@/types/node-definition";
export type { Token, ParserContext, ParseResult, SerializeHelpers, PossibleInlineChildren } from "@/types/node-definition";
