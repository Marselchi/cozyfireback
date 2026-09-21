"use client";

/**
 * Static Slate → React renderer (view pipeline, no Slate editor).
 * Used for read-only page rendering.
 *
 * Reuses the exact same NodeDefinition.render()/decorate-aware renderLeaf()
 * the live editor uses (via the registry), so the view page can never drift
 * from what the editor actually shows — adding/changing a node definition
 * automatically updates this renderer too.
 */

import { Fragment, type ReactNode } from "react";
import type { Descendant } from "slate";
import { Text } from "slate";
import { registry, parseMarkdownToSlate } from "@/lib/editor";
import { cn } from "@/lib/utils";

function renderStaticNode(node: Descendant, key: string): ReactNode {
  // Text leaf — delegate to the registry's mark-aware leaf renderer so
  // bold/italic/underline/strikethrough/code/quote/placeholder styling
  // stays in sync with the editor.
  if (Text.isText(node)) {
    return (
      <Fragment key={key}>
        {registry.renderLeaf({
          attributes: {},
          children: node.text,
          leaf: node,
        } as any)}
      </Fragment>
    );
  }

  const element = node;

  // inline-editor is a void node in the live editor — its real content
  // lives in `element.content`, never in `element.children` (which is just
  // the trivial `[{ text: "" }]` Slate requires for void elements).
  const childNodes: Descendant[] =
    element.type === "inline-editor"
      ? (element.content ?? [])
      : ((element as { children?: Descendant[] }).children ?? []);

  const renderedChildren = childNodes.map((child, i) =>
    renderStaticNode(child, `${key}-${i}`),
  );

  const rendered = registry.renderElement({
    attributes: {},
    element,
    children: renderedChildren,
  } as any);

  // Fallback for any (future) element type without a registered definition:
  // just render its children so content is never silently dropped.
  return <Fragment key={key}>{rendered ?? renderedChildren}</Fragment>;
}

type RenderMarkdownProps = {
  /** Raw markdown source. Parsed via parseMarkdownToSlate before rendering. */
  markdown?: string;
  /** Already-parsed Slate nodes — skips the parse step. */
  nodes?: Descendant[];
  className?: string;
};

/**
 * Static (no Slate editor) React renderer for markdown / Slate Descendant[].
 * Suitable for read-only page views. Pass either `markdown` (parse + render,
 * "Markdown → Slate → React") or pre-parsed `nodes` ("Slate → React").
 */
export function RenderMarkdown({
  markdown,
  nodes,
  className,
}: Readonly<RenderMarkdownProps>) {
  const content = nodes ?? (markdown ? parseMarkdownToSlate(markdown) : []);

  return (
    <div className={cn("prose prose-sm max-w-none", className)}>
      {content.map((node, i) => renderStaticNode(node, String(i)))}
    </div>
  );
}
