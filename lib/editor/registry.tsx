import React from "react";
import type {
  NodeDefinition,
  BlockNodeDefinition,
  InlineNodeDefinition,
  MarkDefinition,
  Token,
  ParserContext,
  ParseResult,
  SerializeHelpers,
} from "@/types/node-definition";
import type { CustomText, CustomRange, CustomElement } from "@/types/editor";
import type { Descendant, NodeEntry } from "slate";
import type { RenderElementProps, RenderLeafProps } from "slate-react";
import type { ReactNode } from "react";
import type MarkdownIt from "markdown-it";

export class NodeRegistry {
  private readonly defs: NodeDefinition[] = [];

  register(def: NodeDefinition): void {
    this.defs.push(def);
  }

  getAll(): NodeDefinition[] {
    return this.defs;
  }

  getBlocks(): BlockNodeDefinition[] {
    return this.defs.filter(
      (d): d is BlockNodeDefinition => d.kind === "block",
    );
  }

  getInlines(): InlineNodeDefinition[] {
    return this.defs.filter(
      (d): d is InlineNodeDefinition => d.kind === "inline",
    );
  }

  getMarks(): MarkDefinition[] {
    return this.defs.filter((d): d is MarkDefinition => d.kind === "mark");
  }

  /** Apply all mdPlugins to a markdown-it instance */
  applyPlugins(md: MarkdownIt): void {
    for (const def of this.defs) {
      if ("mdPlugin" in def && def.mdPlugin) {
        def.mdPlugin(md);
      }
    }
  }

  /** Dispatch token to the right block/inline def */
  parse(token: Token, ctx: ParserContext): ParseResult {
    // Priority: higher first
    const blockDefs = this.getBlocks().sort(
      (a, b) => (b.priority ?? 0) - (a.priority ?? 0),
    );

    for (const def of blockDefs) {
      if (def.match(token, ctx)) {
        const result = def.parse(ctx);
        if (result !== null) return result;
      }
    }

    ctx.advance();
    return null;
  }

  /** Serialize a Slate node to markdown string */
  serialize(node: Descendant, helpers: SerializeHelpers): string | null {
    const allDefs = [...this.getBlocks(), ...this.getInlines()];
    for (const def of allDefs) {
      const result = def.serialize(node, helpers);
      if (result !== null) return result;
    }
    return null;
  }

  /** Render a Slate element to React */
  renderElement(
    props: RenderElementProps & {
      mode?: string;
      roles?: any[];
      paragraphAsDiv?: boolean;
    },
  ): ReactNode | null {
    const element = props.element as CustomElement;
    const allDefs = [...this.getBlocks(), ...this.getInlines()];
    for (const def of allDefs) {
      if (def.type === element.type) {
        return def.render(props);
      }
    }
    return null;
  }

  /** Render a Slate leaf to React */
  renderLeaf(props: RenderLeafProps): ReactNode {
    const leaf = props.leaf as CustomText;
    let { children } = props;

    for (const def of this.getMarks()) {
      if (leaf[def.prop]) {
        const className = def.leafClassName(leaf);
        if (className) {
          children = <span className={className}>{children}</span>;
        }
      }
    }

    return <span {...props.attributes}>{children}</span>;
  }

  /** Run all mark/inline decorate fns for raw mode */
  decorate([node, path]: NodeEntry): CustomRange[] {
    const ranges: CustomRange[] = [];
    if (!node || typeof node !== "object" || !("text" in node)) return ranges;
    const text = (node as { text: string }).text;
    if (!text) return ranges;

    for (const def of this.getMarks()) {
      const newRanges = def.decorate({ text }, path as number[]);
      ranges.push(...newRanges);
    }

    for (const def of this.getInlines()) {
      if (def.decorate) {
        const newRanges = def.decorate({ text }, path as number[]);
        ranges.push(...newRanges);
      }
    }

    return ranges;
  }
}

export const registry = new NodeRegistry();
