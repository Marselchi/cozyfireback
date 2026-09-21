"use client";

import {
  createMarkdownParser,
  MarkdownParserOptions,
} from "@/lib/markdown-parser";
import { useEffect, useRef } from "react";
import { useParams, useRouter } from "next/navigation";
import { RenderMarkdown } from "../editor/render-markdown";
import { useRoomId } from "@/lib/room-utils";

interface TextContentProps {
  content: string;
  mdOptions?: MarkdownParserOptions;
  loreId?: number;
}

export function TextContent({
  content,
  mdOptions,
  loreId,
}: Readonly<TextContentProps>) {
  const containerRef = useRef<HTMLDivElement>(null);
  const roomName = useRoomId();

  // TBF i should remove copy from here and put it in heading node but whateva
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const handleClick = (e: MouseEvent) => {
      const target = e.target as HTMLElement;

      const headerAnchor = target.closest(
        ".header-anchor",
      ) as HTMLAnchorElement;
      if (!headerAnchor) return;

      e.preventDefault();
      e.stopPropagation();

      const href = headerAnchor.getAttribute("href");
      if (!href || !roomName) return;

      const headingId = href.replace(/^#/, "");
      if (!headingId) return;

      let finalLoreId = loreId?.toString();
      if (!finalLoreId) {
        const pathParts = globalThis.location.pathname.split("/");
        const loreIdIndex = pathParts.indexOf("lore");
        if (loreIdIndex !== -1 && pathParts[loreIdIndex + 1]) {
          finalLoreId = pathParts[loreIdIndex + 1];
        }
      }
      if (!finalLoreId) return;

      const fullUrl = `${globalThis.location.origin}/rooms/${roomName}/lore/${finalLoreId}#${headingId}`;

      navigator.clipboard
        .writeText(fullUrl)
        .then(() => {
          const svgElement = headerAnchor.querySelector(
            ".heading-link-icon-svg",
          ) as SVGSVGElement;
          if (!svgElement) return;

          const originalColor = svgElement.style.color;
          svgElement.style.color = "#10b981";
          const originalPaths = Array.from(
            svgElement.querySelectorAll("path"),
          ).map((path) => path.getAttribute("stroke"));

          const paths = svgElement.querySelectorAll("path");
          paths.forEach((path) => {
            path.setAttribute("d", "M20 6 9 17l-5-5");
            path.setAttribute("stroke", "#10b981");
          });

          setTimeout(() => {
            paths.forEach((path, index) => {
              path.setAttribute(
                "d",
                index === 0
                  ? "M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"
                  : "M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71",
              );
              path.setAttribute(
                "stroke",
                originalPaths[index] || "currentColor",
              );
            });
            svgElement.style.color = originalColor;
          }, 2000);
        })
        .catch((err) => {
          console.error("Failed to copy URL: ", err);
        });
    };

    container.addEventListener("click", handleClick);
    return () => container.removeEventListener("click", handleClick);
  }, [roomName, loreId]);

  return (
    <main
      className="markdown prose prose-lg max-w-none mb-12"
      ref={containerRef}
    >
      {RenderMarkdown({
        markdown: content,
        className:
          "prose-headings:text-gray-900 prose-p:text-gray-700 prose-strong:text-gray-900 prose-code:bg-gray-100 prose-code:px-1 prose-code:py-0.5 prose-code:rounded prose-pre:bg-gray-50 prose-blockquote:border-l-4 prose-blockquote:border-gray-300 prose-blockquote:pl-4 prose-blockquote:italic",
      })}
    </main>
  );
}
