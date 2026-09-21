"use client";

import * as React from "react";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { Search, ArrowLeft, X } from "lucide-react";
import { cn } from "@/lib/utils";
import {
  LoreSearchCard,
  LoreSearchCardSkeleton,
} from "@/components/lore/lore-search-card";
import { LoreSearchResult } from "@/types/search";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useRoomId } from "@/lib/room-utils";

const PAGE_SIZE = 6;

// ─── Types ────────────────────────────────────────────────────────────────────

interface SearchState {
  items: LoreSearchResult[];
  totalElements: number;
  nextOffset: number | null;
  loading: boolean;
  loadingMore: boolean;
  query: string;
  searched: boolean;
}

const INITIAL_STATE: SearchState = {
  items: [],
  totalElements: 0,
  nextOffset: null,
  loading: false,
  loadingMore: false,
  query: "",
  searched: false,
};

// Ожидаемый формат ответа от вашего API
interface SearchApiResponse {
  content: LoreSearchResult[];
  totalElements: number;
  nextOffset: number | null;
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function LoreSearchPage() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const roomName = useRoomId();

  const initialQuery = searchParams.get("q") ?? "";

  const [inputValue, setInputValue] = React.useState(initialQuery);
  const [state, setState] = React.useState<SearchState>({
    ...INITIAL_STATE,
    searched: !!initialQuery,
    query: initialQuery,
  });

  const inputRef = React.useRef<HTMLInputElement>(null);
  const observerRef = React.useRef<IntersectionObserver | null>(null);
  const didInitialSearch = React.useRef(false);
  const loadingMoreRef = React.useRef(false);
  const stateRef = React.useRef(state);

  // ── Focus on mount ──────────────────────────────────────────────────────────
  React.useEffect(() => {
    inputRef.current?.focus();
  }, []);

  // ── Run initial search from URL param ──────────────────────────────────────
  React.useEffect(() => {
    if (initialQuery && !didInitialSearch.current) {
      didInitialSearch.current = true;
      runSearch(initialQuery);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ── Core search: always resets cursor to offset 0 ──────────────────────────
  async function runSearch(query: string) {
    const trimmed = query.trim();

    // 1. Упрощенное обновление URL через Next.js router
    if (trimmed) {
      router.replace(`?q=${encodeURIComponent(trimmed)}`);
    } else {
      router.replace(window.location.pathname);
    }

    if (!trimmed) {
      setState({ ...INITIAL_STATE, searched: true });
      return;
    }

    setState((s) => ({
      ...s,
      loading: true,
      searched: true,
      query: trimmed,
      items: [],
      totalElements: 0,
      nextOffset: null,
    }));

    try {
      // 2. Формирование параметров запроса
      const searchParams = new URLSearchParams({
        roomName: roomName,
        q: trimmed,
        offset: "0",
        limit: PAGE_SIZE.toString(),
      });

      // 3. Реальный fetch запрос к API
      const response = await fetch(
        `/api/lore/full-search?${searchParams.toString()}`,
        {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
          },
        },
      );

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const res: SearchApiResponse = await response.json();

      setState({
        items: res.content,
        totalElements: res.totalElements,
        nextOffset: res.nextOffset,
        loading: false,
        loadingMore: false,
        query: trimmed,
        searched: true,
      });
    } catch (error) {
      console.error("Search failed:", error);
      setState((s) => ({ ...s, loading: false }));
      // Здесь можно добавить установку состояния ошибки для отображения UI
    }
  }

  React.useEffect(() => {
    stateRef.current = state;
  }, [state]);

  // ── Load next page (infinite scroll) ───────────────────────────────────────
  const loadMore = React.useCallback(async () => {
    const s = stateRef.current;
    if (s.nextOffset === null || s.loading || loadingMoreRef.current) return;

    loadingMoreRef.current = true;
    setState((prev) => ({ ...prev, loadingMore: true }));

    try {
      const searchParams = new URLSearchParams({
        roomName: roomName,
        q: s.query,
        offset: String(s.nextOffset),
        limit: PAGE_SIZE.toString(),
      });

      const response = await fetch(
        `/api/lore/full-search?${searchParams.toString()}`,
        {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
          },
        },
      );

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const res: SearchApiResponse = await response.json();

      setState((prev) => ({
        ...prev,
        items: [...prev.items, ...res.content],
        nextOffset: res.nextOffset,
        loadingMore: false,
      }));
    } catch (error) {
      console.error("Load more failed:", error);
      setState((prev) => ({ ...prev, loadingMore: false }));
    } finally {
      loadingMoreRef.current = false;
    }
  }, []);

  const sentinelCallback = React.useCallback(
    (node: HTMLDivElement | null) => {
      if (observerRef.current) {
        observerRef.current.disconnect();
        observerRef.current = null;
      }
      if (!node) return;

      observerRef.current = new IntersectionObserver(
        (entries) => {
          if (entries[0].isIntersecting) loadMore();
        },
        { rootMargin: "200px" },
      );
      observerRef.current.observe(node);
    },
    [loadMore],
  );

  // ── Input handlers ─────────────────────────────────────────────────────────
  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter") runSearch(inputValue);
  }

  function handleClear() {
    setInputValue("");
    setState({ ...INITIAL_STATE });
    // Очищаем URL от query-параметров через router
    router.replace(window.location.pathname);
    inputRef.current?.focus();
  }

  // ── Derived display values ─────────────────────────────────────────────────
  const {
    items,
    totalElements,
    loading,
    loadingMore,
    query,
    searched,
    nextOffset,
  } = state;
  const hasMore = nextOffset !== null;
  const shownCount = items.length;

  return (
    <div className="min-h-screen bg-background">
      {/* ── Sticky top bar ── */}
      <header className="sticky top-[calc(var(--navbar-height))] z-10 bg-background/90 backdrop-blur-sm border-b border-border">
        <div className="max-w-4/5 mx-auto px-4 py-3 flex items-center gap-3">
          {/* Search input */}
          <div className="relative flex-1">
            <Search
              className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none"
              aria-hidden
            />
            <Input
              ref={inputRef}
              type="search"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Поиск контента…"
              className={cn(
                "w-full rounded-(--radius) bg-secondary border border-input",
                "pl-9 pr-10 py-2 text-sm",
                "placeholder:text-muted-foreground",
                "focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent",
                "transition-shadow",
              )}
              aria-label="Поиск лора по содержимому"
              autoComplete="off"
              spellCheck={false}
            />
            {inputValue && (
              <button
                onClick={handleClear}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                aria-label="Очистить поиск"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            )}
          </div>

          {/* Search button */}
          <Button
            onClick={() => runSearch(inputValue)}
            disabled={loading}
            className={cn(
              "shrink-0 px-4 py-2 rounded-(--radius) text-sm font-medium",
              "bg-primary text-primary-foreground",
              "hover:opacity-90 active:scale-[0.98]",
              "disabled:opacity-50 disabled:cursor-not-allowed",
              "transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",
            )}
          >
            Поиск
          </Button>
        </div>

        {/* Status line — sticky alongside the input */}
        {searched && !loading && (
          <div className="max-w-4/5 mx-auto px-4 pb-2">
            <p className="text-xs text-muted-foreground">
              {totalElements === 0 && query
                ? `Нет результатов для "${query}"`
                : totalElements === 0
                  ? "Введите поисковую фразу выше."
                  : `Отображается ${shownCount} результатов для "${query}" из ${totalElements}`}
            </p>
          </div>
        )}
      </header>

      {/* ── Results area ── */}
      <main className="max-w-3/4 mx-auto px-4 py-6 space-y-3">
        {loading &&
          Array.from({ length: 4 }).map((_, i) => (
            <LoreSearchCardSkeleton key={i} />
          ))}

        {!loading && searched && totalElements === 0 && (
          <div className="flex flex-col items-center justify-center py-24 text-center gap-3">
            <Search className="h-10 w-10 text-muted-foreground/40" />
            <p className="text-muted-foreground text-sm">
              {query ? (
                <>
                  Нет лора содержащего{" "}
                  <span className="font-medium text-foreground">
                    &quot;{query}&quot;
                  </span>
                  .
                </>
              ) : (
                "Введите поисковую фразу выше."
              )}
            </p>
          </div>
        )}

        {!searched && (
          <div className="flex flex-col items-center justify-center py-24 text-center gap-3">
            <Search className="h-10 w-10 text-muted-foreground/40" />
            <p className="text-muted-foreground text-sm">
              Напишите что-то и оно найдется.
            </p>
          </div>
        )}

        {!loading &&
          items.map((result) => (
            <LoreSearchCard key={result.id} result={result} />
          ))}

        {loadingMore &&
          Array.from({ length: 3 }).map((_, i) => (
            <LoreSearchCardSkeleton key={`more-${i}`} />
          ))}

        {hasMore && !loadingMore && (
          <div ref={sentinelCallback} className="h-8" aria-hidden />
        )}

        {!loading && !loadingMore && !hasMore && items.length > 0 && (
          <p className="text-center text-xs text-muted-foreground py-4">
            Все {totalElements} результаты отображены.
          </p>
        )}
      </main>
    </div>
  );
}
