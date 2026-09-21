"use client";

import { useMemo, useRef, useCallback } from "react";
import { createMarkdownParser } from "@/lib/markdown-parser";
import { serializeToMarkdownPreview } from "@/lib/editor-utils";
import { useEditorState } from "../editor/editor-state-provider";
import { RenderMarkdown } from "../editor/render-markdown";

export function PreviewBlock() {
  const previewValue = useEditorState((s) => s.previewValue);
  const previewRef = useRef<HTMLDivElement>(null);
  const scrollPositionRef = useRef(0);
  const contentRef = useRef<HTMLDivElement>(null);
  const handleScroll = useCallback(() => {
    if (previewRef.current) {
      scrollPositionRef.current = previewRef.current.scrollTop;
    }
  }, []);

  const renderedContent = useMemo(() => {
    return RenderMarkdown({ nodes: previewValue });
  }, [previewValue]);

  return (
    <div
      className="h-full overflow-y-scroll max-h-screen"
      ref={previewRef}
      onScroll={handleScroll}
      style={{ height: "100%" }}
    >
      <div className="px-3 py-2 min-h-full">
        <div
          className="rounded-lg shadow-xs mb-12 p-6 min-h-full"
          ref={contentRef}
        >
          {renderedContent ? (
            renderedContent
          ) : (
            <div className="text-center py-12">
              <p className="text-lg mb-2">
                Начните писать чтобы увидеть превью
              </p>
              <p className="text-sm">Ваш контент отобразится здесь</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
