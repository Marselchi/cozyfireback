import type { MarkDefinition } from "@/types/node-definition";
import type { CustomText, CustomRange } from "@/types/editor";

function makeRange(
  text: string,
  path: number[],
  regex: RegExp,
  flags: Partial<CustomRange>,
): CustomRange[] {
  const ranges: CustomRange[] = [];
  const re = new RegExp(regex.source, regex.flags);
  let match: RegExpExecArray | null;

  while ((match = re.exec(text)) !== null) {
    const start = match.index;
    const end = start + match[0].length;

    // Overlap check
    const overlaps = ranges.some(
      (r) =>
        (start >= r.anchor.offset && start < r.focus.offset) ||
        (end > r.anchor.offset && end <= r.focus.offset),
    );
    if (!overlaps) {
      ranges.push({
        anchor: { path, offset: start },
        focus: { path, offset: end },
        ...flags,
      });
    }
  }

  return ranges;
}

export const boldDef: MarkDefinition = {
  type: "bold",
  kind: "mark",
  prop: "bold",
  mdMark: "strong",

  wrap(inner: string): string {
    return `**${inner}**`;
  },

  decorate({ text }: { text: string }, path: number[]): CustomRange[] {
    return makeRange(text, path, /\*\*([^*]+?)\*\*/g, { bold: true });
  },

  leafClassName(leaf: CustomText): string | false {
    return leaf.bold ? "font-bold text-foreground" : false;
  },
};

export const italicDef: MarkDefinition = {
  type: "italic",
  kind: "mark",
  prop: "italic",
  mdMark: "em",

  wrap(inner: string): string {
    return `_${inner}_`;
  },

  decorate({ text }: { text: string }, path: number[]): CustomRange[] {
    return makeRange(text, path, /_([^_]+?)_/g, { italic: true });
  },

  leafClassName(leaf: CustomText): string | false {
    return leaf.italic ? "italic text-foreground" : false;
  },
};

export const boldItalicDef: MarkDefinition = {
  type: "bold-italic",
  kind: "mark",
  prop: "bold",

  wrap(inner: string): string {
    return `***${inner}***`;
  },

  decorate({ text }: { text: string }, path: number[]): CustomRange[] {
    return makeRange(text, path, /\*\*\*([^*]+?)\*\*\*/g, {
      bold: true,
      italic: true,
    });
  },

  leafClassName(): string | false {
    return false; // handled by boldDef + italicDef individually
  },
};

export const underlineDef: MarkDefinition = {
  type: "underline",
  kind: "mark",
  prop: "underline",

  wrap(inner: string): string {
    return `__${inner}__`;
  },

  decorate({ text }: { text: string }, path: number[]): CustomRange[] {
    return makeRange(text, path, /__([^_]+?)__/g, {});
  },

  leafClassName(leaf: CustomText): string | false {
    return leaf.underline ? "underline" : false;
  },
};

export const strikethroughDef: MarkDefinition = {
  type: "strikethrough",
  kind: "mark",
  prop: "strikethrough",

  wrap(inner: string): string {
    return `~~${inner}~~`;
  },

  decorate({ text }: { text: string }, path: number[]): CustomRange[] {
    return makeRange(text, path, /~~([^~]+?)~~/g, { strike: true });
  },

  leafClassName(leaf: CustomText): string | false {
    return leaf.strikethrough ? "line-through text-muted-foreground" : false;
  },
};

export const codeDef: MarkDefinition = {
  type: "code",
  kind: "mark",
  prop: "code",
  mdMark: "code",

  wrap(inner: string): string {
    return `\`${inner}\``;
  },

  decorate({ text }: { text: string }, path: number[]): CustomRange[] {
    return makeRange(text, path, /`([^`]+)`/g, { code: true });
  },

  leafClassName(leaf: CustomText): string | false {
    return leaf.code
      ? "font-mono bg-muted text-pink-600 dark:text-pink-400 px-0.5 rounded"
      : false;
  },
};

export const quoteDef: MarkDefinition = {
  type: "quote",
  kind: "mark",
  prop: "quote",

  wrap(inner: string): string {
    return `<? ${inner}`;
  },

  decorate(): CustomRange[] {
    return [];
  },

  leafClassName(leaf: CustomText): string | false {
    return leaf.quote ? "bg-primary/10 italic" : false;
  },
};

export const placeholderMarkDef: MarkDefinition = {
  type: "placeholder-mark",
  kind: "mark",
  prop: "placeholder",

  wrap(inner: string): string {
    return inner; // already includes {{}}
  },

  decorate({ text }: { text: string }, path: number[]): CustomRange[] {
    return makeRange(text, path, /\{\{([^}]+?)\}\}/g, { placeholder: true });
  },

  leafClassName(leaf: CustomText): string | false {
    return leaf.placeholder ? "bg-muted text-orange-400" : false;
  },
};

export const markDef: MarkDefinition = {
  type: "mark",
  kind: "mark",
  prop: "mark",

  wrap(inner: string): string {
    return `==${inner}==`;
  },

  decorate({ text }: { text: string }, path: number[]): CustomRange[] {
    return makeRange(text, path, /==([^=]+)==/g, { code: true });
  },

  leafClassName(leaf: CustomText): string | false {
    return leaf.mark ? "bg-yellow-200 dark:bg-yellow-900" : false;
  },
};

export const allMarkDefs: MarkDefinition[] = [
  markDef,
  boldItalicDef,
  boldDef,
  italicDef,
  underlineDef,
  strikethroughDef,
  codeDef,
  quoteDef,
  placeholderMarkDef,
];
