import type { BaseEditor, Descendant, Point } from "slate";
import type { ReactEditor } from "slate-react";
import type { HistoryEditor } from "slate-history";
import { Role } from "./editor-layout";

export type EditorMode = "raw" | "parsed";

export const emptyContent: Descendant[] = [
  { type: "paragraph" as const, children: [{ text: "" }] },
];

export type CustomText = {
  text: string;
  bold?: boolean;
  italic?: boolean;
  underline?: boolean;
  strikethrough?: boolean;
  code?: boolean;
  quote?: boolean;
  placeholder?: boolean;
  softbreak?: boolean;
  mark?: boolean;
};

export type CustomRange = {
  anchor: Point;
  focus: Point;
  bold?: boolean;
  italic?: boolean;
  code?: boolean;
  heading?: boolean;
  list?: boolean;
  hr?: boolean;
  blockquote?: boolean;
  punctuation?: boolean;
  url?: boolean;
  strike?: boolean;
  quote?: boolean;
  placeholder?: boolean;
  mark?: boolean;
};

export type ParagraphElement = {
  type: "paragraph";
  children: (CustomText | LinkElement | ImageElement | DiceElement)[];
};

export type HeadingElement = {
  type: "heading";
  level: 1 | 2 | 3 | 4 | 5 | 6;
  children: (CustomText | LinkElement | ImageElement | DiceElement)[];
};

export type BlockquoteElement = {
  type: "blockquote";
  children: (CustomText | LinkElement | ImageElement | DiceElement)[];
};

export type QuoteBlockElement = {
  type: "quote-block";
  children: (CustomText | LinkElement | ImageElement | DiceElement)[];
};

export type CodeBlockElement = {
  type: "code-block";
  children: CustomText[];
};

export type ListItemElement = {
  type: "list-item";
  children: (CustomText | LinkElement | ImageElement | DiceElement)[];
};

export type TableElement = {
  type: "table";
  children: (CustomText | LinkElement | ImageElement | DiceElement)[];
};

export type TableRowElement = {
  type: "table-row";
  children: (CustomText | LinkElement | ImageElement | DiceElement)[];
};

export type TableCellElement = {
  type: "table-cell";
  children: (CustomText | LinkElement | ImageElement | DiceElement)[];
};

export type BulletedListElement = {
  type: "bulleted-list";
  children: ListItemElement[];
};

export type NumberedListElement = {
  type: "numbered-list";
  children: ListItemElement[];
};

export type LinkElement = {
  type: "link";
  url: string;
  children: CustomText[];
};

export type ImageElement = {
  type: "image";
  src: string;
  alt: string;
  title?: string;
  children: [{ text: "" }];
};

export type SpoilerType = "normal" | "chance";

export type ChanceMetadata = {
  skill: string;
  threshold: number;
};

export type BaseMetadata = {
  id?: string;
  roles?: string[];
};

export type NormalMetadata = BaseMetadata & {
  spoilerType: "normal";
  chance?: never;
};

export type ChanceMetadataBlock = BaseMetadata & {
  spoilerType: "chance";
  chance: ChanceMetadata;
};

export type InlineMetadata = NormalMetadata | ChanceMetadataBlock;

export type InlineEditorElement = {
  type: "inline-editor";
  id?: string;
  realId?: string;
  spoilerType: SpoilerType;
  chance?: ChanceMetadata;
  roles: string[];
  content: Descendant[];
  children: CustomText[];
};

export type RollableElement = {
  type: "rollable";
  id?: string;
  rollId?: number;
  children: CustomText[];
};

/**
 * Inline dice formula: "{2d6+5}" or "{2к6+5}" ("к" — Cyrillic ka — is an
 * accepted alias for "d"). Sits inline among regular text, e.g.
 * "This enemy can attack {2d6-3} or use another {7к2+2} attack". Clicking
 * the rendered chip rolls `count` dice of `sides` sides and adds `modifier`.
 */
export type DiceElement = {
  type: "dice";
  id?: string;
  count: number;
  sides: number;
  modifier: number;
  /** Which separator letter was used in the source text; purely cosmetic
   * for round-tripping back to markdown. Defaults to "d". */
  letter?: "d" | "к";
  children: CustomText[];
};

export type CustomElement =
  | ParagraphElement
  | HeadingElement
  | BlockquoteElement
  | QuoteBlockElement
  | CodeBlockElement
  | ListItemElement
  | BulletedListElement
  | NumberedListElement
  | InlineEditorElement
  | LinkElement
  | TableElement
  | TableRowElement
  | TableCellElement
  | ImageElement
  | RollableElement
  | DiceElement;

declare module "slate" {
  interface CustomTypes {
    Editor: BaseEditor & ReactEditor & HistoryEditor;
    Element: CustomElement;
    Text: CustomText;
    Range: CustomRange;
  }
}

export type EditorContent = {
  id?: number;
  content?: Descendant[];
  roles: Role[];
};

export type MarkdownExport = {
  markdown: string;
  inlineEditors: {
    id: string;
    roles: string[];
    markdown: string;
  }[];
};
