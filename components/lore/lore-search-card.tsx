"use client";

import * as React from "react";
import { Badge } from "@/components/ui/badge";
import { Shield, ChevronDown, Lock } from "lucide-react";
import { cn } from "@/lib/utils";
import { RenderMarkdown } from "../editor/render-markdown";
import { LoreMatchChunk, LoreSearchResult } from "@/types/search";
import CustomLink from "../no-prefetch-link";

// ─── Constants ────────────────────────────────────────────────────────────────

/** How many match chunks to show before the "X more" button */
const INITIAL_SHOW = 2;
/** How many additional chunks to reveal per click */
const LOAD_MORE_STEP = 3;

// ─── Snippet ──────────────────────────────────────────────────────────────────

function Snippet({
  content,
  occurrenceCount,
}: Readonly<{
  content: string;
  occurrenceCount: number;
}>) {
  return (
    <div className="flex items-start gap-2">
      <div className="flex-1 text-sm text-muted-foreground leading-relaxed">
        {RenderMarkdown({
          markdown: content,
          className:
            "prose-headings:text-gray-900 prose-p:text-gray-700 prose-strong:text-gray-900 prose-code:bg-gray-100 prose-code:px-1 prose-code:py-0.5 prose-code:rounded prose-pre:bg-gray-50 prose-blockquote:border-l-4 prose-blockquote:border-gray-300 prose-blockquote:pl-4 prose-blockquote:italic",
        })}
      </div>
      {occurrenceCount > 1 && (
        <span
          className="shrink-0 mt-0.5 text-xs font-medium tabular-nums text-muted-foreground bg-secondary rounded-full px-2 py-0.5"
          title={`${occurrenceCount} вхождений`}
        >
          ×{occurrenceCount}
        </span>
      )}
    </div>
  );
}

// ─── Skeleton ─────────────────────────────────────────────────────────────────

export function LoreSearchCardSkeleton() {
  return (
    <div className="rounded-(--radius) bg-card border border-border px-5 py-4 space-y-3 animate-pulse">
      <div className="flex items-center gap-2">
        <div className="h-5 w-10 rounded-full bg-muted" />
        <div className="h-5 w-48 rounded bg-muted" />
        <div className="ml-auto h-5 w-14 rounded-full bg-muted" />
      </div>
      <div className="space-y-1.5">
        <div className="h-3.5 w-full rounded bg-muted" />
        <div className="h-3.5 w-[90%] rounded bg-muted" />
        <div className="h-3.5 w-[75%] rounded bg-muted" />
      </div>
      <div className="space-y-1.5 pt-3 border-t border-border/50">
        <div className="h-3.5 w-full rounded bg-muted" />
        <div className="h-3.5 w-[80%] rounded bg-muted" />
      </div>
    </div>
  );
}

// ─── Main card ────────────────────────────────────────────────────────────────

export interface LoreSearchCardProps {
  result: LoreSearchResult;
}

export function LoreSearchCard({ result }: Readonly<LoreSearchCardProps>) {
  const [visibleCount, setVisibleCount] = React.useState(INITIAL_SHOW);

  const target = result.href ?? `./${result.id}`;
  const visibleMatches: LoreMatchChunk[] = result.matches.slice(
    0,
    visibleCount,
  );
  const hiddenCount = result.matches.length - visibleCount;
  const serverHasMore = result.notAll && visibleCount >= result.matches.length;
  const remainingToShow = Math.min(hiddenCount, LOAD_MORE_STEP);

  function handleLoadMore(e: React.MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    setVisibleCount((v) => v + LOAD_MORE_STEP);
  }

  return (
    <CustomLink
      href={target}
      className={cn(
        "group block rounded-(--radius) bg-card border border-border",
        "px-5 py-4 space-y-3",
        "transition-all duration-200 hover:border-primary/40 hover:shadow-sm hover:-translate-y-px",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",
      )}
    >
      {/* ── Header: badges + title + total occurrence count ── */}
      <div className="flex items-center gap-2 flex-wrap">
        {result.byAdmin && (
          <Badge
            variant="outline"
            className="border-[hsl(var(--lore-admin,220_90%_56%))] text-[hsl(var(--lore-admin,220_90%_56%))] shrink-0 px-2 py-0.5 text-xs"
          >
            <Shield className="h-3 w-3 mr-1" />
            DM
          </Badge>
        )}
        {result.secret && (
          <Badge
            variant="outline"
            className="border-muted-foreground/40 text-muted-foreground shrink-0 px-2 py-0.5 text-xs"
          >
            <Lock className="h-3 w-3 mr-1" />
            Скрытое
          </Badge>
        )}

        <h3 className="flex-1 font-semibold text-base leading-tight text-balance group-hover:text-primary transition-colors min-w-0">
          {result.title}
        </h3>

        <span
          className="shrink-0 text-xs font-medium tabular-nums text-muted-foreground bg-secondary rounded-full px-2.5 py-0.5"
          title={`${result.totalOccurrences} совпадений${result.notAll ? " (не все отображены)" : ""}`}
        >
          Всего:
          {result.totalOccurrences}
          {result.notAll ? "+" : ""}
        </span>
      </div>

      {/* ── Match snippets ── */}
      <div className="space-y-3">
        {visibleMatches.map((chunk, i) => (
          <div key={i} className={cn(i > 0 && "pt-3 border-t border-input")}>
            <Snippet
              content={chunk.matchContent}
              occurrenceCount={chunk.occurrenceCount}
            />
          </div>
        ))}
      </div>

      {/* ── Load more occurrences button ── */}
      {(hiddenCount > 0 || serverHasMore) && (
        <button
          onClick={handleLoadMore}
          className={cn(
            "mt-1 flex items-center gap-1.5 text-xs font-medium",
            "text-muted-foreground hover:text-foreground transition-colors",
            "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring rounded",
            serverHasMore && "opacity-50 cursor-not-allowed",
          )}
          disabled={serverHasMore}
          aria-label={
            serverHasMore
              ? "Слишком много совпадений: откройте полную статью"
              : `Отобразить еще ${remainingToShow} совпадений`
          }
        >
          <ChevronDown className="h-3.5 w-3.5" />
          {serverHasMore
            ? "Больше совпадений"
            : `Открыть еще ${hiddenCount} совпадение`}
        </button>
      )}
    </CustomLink>
  );
}
