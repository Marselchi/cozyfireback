"use client";

import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import type { Excerpt } from "@/types/lore";
import { LoreTarget } from "@/lib/editor/util/link-util";
import { RenderMarkdown } from "../editor/render-markdown";

// TODO: point this at your real API.
async function fetchLoreExcerpt(target: LoreTarget): Promise<Excerpt> {
  // const res = await fetch(
  //   `/api/rooms/${target.roomName}/lore/${target.loreId}/excerpt?heading=${encodeURIComponent(target.heading)}`,
  // );
  // if (!res.ok) {
  //   throw new Error(`Failed to fetch lore excerpt (${res.status})`);
  // }
  // return res.json();
  return {
    loreId: 1,
    title: "Не доделал",
    content: "Prostite, будет вместе с поиском",
    href: `../lore/${target.loreId}`,
  };
}

type LoreLinkProps = {
  attributes?: Record<string, unknown>;
  href: string;
  className: string;
  lore: LoreTarget;
  children: React.ReactNode;
};

export function LoreLink({
  attributes,
  href,
  className,
  lore,
  children,
}: Readonly<LoreLinkProps>) {
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef<HTMLSpanElement>(null);
  const router = useRouter();

  // Lazy — only fetches once the popup is actually opened, and caches by
  // (room, loreId, heading) so reopening the same heading is instant.
  const { data, isLoading, isError } = useQuery({
    queryKey: ["lore-excerpt", lore.roomName, lore.loreId, lore.heading],
    queryFn: () => fetchLoreExcerpt(lore),
    enabled: open,
  });

  return (
    <span ref={wrapperRef} className="relative inline-block">
      <button
        {...attributes}
        className={className}
        onClick={(e) => {
          e.preventDefault();
          setOpen((v) => !v);
        }}
      >
        {children}
      </button>

      {open && (
        <div className="absolute z-10 bottom-full left-1/2 mb-2 -translate-x-1/2 bg-accent rounded-lg shadow-2xl border border-input p-4 max-w-md w-80 markdown-inline">
          {isLoading && <LoreLinkSkeleton />}

          {isError && (
            <div className="text-sm text-destructive">
              Couldn&apos;t load this excerpt.
            </div>
          )}

          {data && (
            <>
              <div className="text-sm mb-3 prose prose-sm max-w-none">
                {RenderMarkdown({
                  markdown: data.content,
                })}
              </div>
              <button
                type="button"
                onClick={() => {
                  setOpen(false);
                  router.push(data.href);
                }}
                className="text-sm text-blue-600 hover:text-blue-500 hover:underline inline-block"
              >
                → {data.title}
              </button>
            </>
          )}
        </div>
      )}
    </span>
  );
}

function LoreLinkSkeleton() {
  return (
    <div className="animate-pulse space-y-2">
      <div className="h-3 bg-muted rounded w-3/4" />
      <div className="h-3 bg-muted rounded w-full" />
      <div className="h-3 bg-muted rounded w-5/6" />
      <div className="h-3 bg-muted rounded w-2/3 mt-3" />
    </div>
  );
}
