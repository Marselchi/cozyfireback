"use client";

import type { RenderLeafProps } from "slate-react";
import { registry } from "@/lib/editor";

/**
 * Slate → React leaf renderer.
 * All mark styling (bold/italic/underline/strikethrough/code/quote/
 * placeholder, plus raw-mode decoration ranges) now lives exclusively in
 * each MarkDefinition.leafClassName() inside lib/editor/nodes/marks.ts.
 * This component is just the registry → Slate wiring.
 */
export function LeafRenderer(props: RenderLeafProps) {
  return <>{registry.renderLeaf(props)}</>;
}
