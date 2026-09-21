import type { Descendant, NodeEntry } from "slate";
import type { RenderElementProps, RenderLeafProps } from "slate-react";
import type { ReactNode } from "react";
import type {
  CustomText,
  LinkElement,
  ImageElement,
  DiceElement,
  CustomRange,
  CustomElement,
} from "./editor";
import type MarkdownIt from "markdown-it";

// ── Token (mirrors markdown-it Token) ──────────────────────────────────────

export interface Token {
  type: string;
  tag: string;
  attrs: [string, string][] | null;
  map: [number, number] | null;
  nesting: -1 | 0 | 1;
  level: number;
  children: Token[] | null;
  content: string;
  meta: Record<string, any> | null;
  info: string;
  hidden: boolean;
  attrGet(name: string): string | null;
  attrSet(name: string, value: string): void;
  attrPush(attr: [string, string]): void;
  attrJoin(name: string, value: string): void;
}

// ── Parser context ──────────────────────────────────────────────────────────

export type PossibleInlineChildren =
  | CustomText
  | LinkElement
  | ImageElement
  | DiceElement;
export type ParseResult = Descendant | Descendant[] | null;

export interface ParserContext {
  tokens: Token[];
  index: number;
  current(): Token | null;
  next(): Token | null;
  peek(offset?: number): Token | null;
  advance(): void;
  rewind(steps?: number): void;
  done(): boolean;
  parseChildren(until?: string[], stopOnSameLevel?: boolean): Descendant[];
  parseInline(tokens: Token[]): PossibleInlineChildren[];
  extractText(tokens: Token[]): string;
}

// ── Serialize helpers ──────────────────────────────────────────────────────

export interface SerializeHelpers {
  serializeChildren(nodes: Descendant[]): string;
  serializeNode(node: Descendant): string | null;
  isPreview: boolean;
}

// ── Node definitions ───────────────────────────────────────────────────────

type BaseDef = {
  type: string;
  priority?: number;
};

export interface BlockNodeDefinition extends BaseDef {
  kind: "block";
  mdPlugin?: (md: MarkdownIt) => void;
  match(token: Token, ctx: ParserContext): boolean;
  parse(ctx: ParserContext): ParseResult;
  serialize(node: Descendant, helpers: SerializeHelpers): string | null;
  render(
    props: RenderElementProps & {
      mode?: string;
      roles?: any[];
      paragraphAsDiv?: boolean;
    },
  ): ReactNode;
}

export interface InlineNodeDefinition extends BaseDef {
  kind: "inline";
  mdPlugin?: (md: MarkdownIt) => void;
  matchInline(token: Token): boolean;
  parseInline(token: Token, ctx: ParserContext): PossibleInlineChildren;
  serialize(node: Descendant, helpers: SerializeHelpers): string | null;
  render(
    props: RenderElementProps & {
      mode?: string;
      roles?: any[];
      paragraphAsDiv?: boolean;
    },
  ): ReactNode;
  decorate?(text: { text: string }, path: number[]): CustomRange[];
}

export interface MarkDefinition extends BaseDef {
  kind: "mark";
  prop: keyof Omit<CustomText, "text">;
  mdMark?: string;
  mdPlugin?: (md: MarkdownIt) => void;
  wrap(inner: string, node: CustomText): string;
  decorate(text: { text: string }, path: number[]): CustomRange[];
  leafClassName(leaf: CustomText): string | false;
}

export type NodeDefinition =
  | BlockNodeDefinition
  | InlineNodeDefinition
  | MarkDefinition;
