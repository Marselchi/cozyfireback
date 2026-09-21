"use client";

import { Suspense, useState } from "react";
import { Search, Dices, Lock, Hash } from "lucide-react";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { ErrorBoundary } from "@/components/error-boundary";
import { BlockListSkeleton } from "./block-list-skeleton";
import { useBlockList } from "@/lib/blocks/queries";
import { useInfiniteScrollSentinel } from "@/hooks/use-infinite-scroll-sentinel";
import { Toggle } from "@/components/ui/toggle";
import { cn } from "@/lib/utils";
import { BlockType } from "@/types/blocks";
import { useRoomId } from "@/lib/room-utils";
import useDebounce from "@/hooks/use-debounce";

type SearchMode = "title" | "id";

function BlockListContent({
  filters,
  searchMode,
  selectedId,
  onSelect,
}: Readonly<{
  filters: { type?: BlockType; search?: string; id?: string };
  searchMode: SearchMode;
  selectedId: string | null;
  onSelect: (id: string) => void;
}>) {
  const roomId = useRoomId();
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage } = useBlockList(
    roomId,
    filters,
  );

  const allBlocks = data.pages.flatMap((page) => page.content);

  const sentinelRef = useInfiniteScrollSentinel({
    onIntersect: () => {
      if (hasNextPage && !isFetchingNextPage) fetchNextPage();
    },
    enabled: hasNextPage ?? false,
  });

  if (allBlocks.length === 0) {
    const activeQuery = searchMode === "id" ? filters.id : filters.search;
    return (
      <div className="flex flex-col items-center gap-2 px-6 py-12 text-center">
        <p className="text-sm text-muted-foreground">
          Не найдено блоков для лора
          {activeQuery
            ? searchMode === "id"
              ? ` с ID "${activeQuery}"`
              : ` с названием "${activeQuery}"`
            : ""}
          .
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-2 p-3">
      {allBlocks.map((block) => (
        <button
          key={block.id}
          onClick={() => onSelect(block.id)}
          className={cn(
            "flex flex-col gap-1.5 rounded-lg border p-3 text-left transition-colors",
            "hover:border-primary/40 hover:bg-primary/5",
            selectedId === block.id
              ? "border-primary/50 bg-primary/10"
              : "border-border bg-card",
          )}
        >
          <div className="flex items-center justify-between gap-2">
            <span className="text-sm font-medium text-foreground">
              {block.title}
            </span>
            {block.type === "chance" ? (
              <span className="flex items-center gap-1 text-[11px] font-medium text-accent-foreground">
                <Dices className="size-3" />
                Шанс
              </span>
            ) : (
              <span className="flex items-center gap-1 text-[11px] font-medium text-muted-foreground">
                <Lock className="size-3" />
                Обычный
              </span>
            )}
          </div>
          <p className="line-clamp-2 text-xs leading-relaxed text-muted-foreground">
            {block.contentPreview}
          </p>
          <span className="font-mono text-[10px] text-muted-foreground/70">
            {block.loreId}
          </span>
        </button>
      ))}
      {hasNextPage && (
        <div
          ref={sentinelRef}
          className="flex justify-center py-2 text-xs text-muted-foreground"
        >
          {isFetchingNextPage ? "Грузим больше…" : ""}
        </div>
      )}
    </div>
  );
}

export function BlockListPanel({
  selectedId,
  onSelect,
}: Readonly<{
  selectedId: string | null;
  onSelect: (id: string) => void;
}>) {
  // Local, immediate value for the input itself — so typing feels instant —
  // while the value actually passed into the query (and therefore the
  // query key / network request) is debounced. Same pattern as the lore
  // and question search inputs elsewhere in the app.
  const [searchInput, setSearchInput] = useState("");
  const debouncedSearch = useDebounce(searchInput, 300);

  const [searchMode, setSearchMode] = useState<SearchMode>("title");
  const [selectedType, setSelectedType] = useState<BlockType | undefined>();

  const trimmed = debouncedSearch.trim() || undefined;
  const filters = {
    type: selectedType,
    search: searchMode === "title" ? trimmed : undefined,
    id: searchMode === "id" ? trimmed : undefined,
  };

  const handleTypeToggle = (type: BlockType) => {
    setSelectedType((prev) => (prev === type ? undefined : type));
  };

  const handleSearchModeToggle = () => {
    setSearchMode((prev) => (prev === "title" ? "id" : "title"));
  };

  const isIdMode = searchMode === "id";

  return (
    <div className="flex h-full flex-col">
      <div className="space-y-2 border-b border-border p-3">
        <div className="flex gap-1.5">
          <div className="relative flex-1">
            {isIdMode ? (
              <Hash className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
            ) : (
              <Search className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
            )}
            <Input
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder={isIdMode ? "Найти блок по ID" : "Фильтр по названию"}
              className="h-9 pl-8 text-sm"
            />
          </div>
          {/* Search-mode switch: title vs exact id. The type filters below
              stay visible and usable in either mode — this only changes
              which query param the text field feeds. */}
          <Toggle
            pressed={isIdMode}
            onPressedChange={handleSearchModeToggle}
            size="lg"
            className="shrink-0 border border-input"
            aria-label={
              isIdMode
                ? "Переключиться на поиск по названию"
                : "Переключиться на поиск по ID"
            }
            title={
              isIdMode
                ? "Поиск по ID (нажмите для поиска по названию)"
                : "Поиск по названию (нажмите для поиска по ID)"
            }
          >
            <Hash className="size-4" />
          </Toggle>
        </div>
        <div className="flex gap-1">
          <Toggle
            pressed={selectedType === "normal"}
            onPressedChange={() => handleTypeToggle("normal")}
            size="sm"
            className="flex-1 border border-input"
          >
            <Lock className="size-3" />
            <span className="ml-1 text-xs">Обычный</span>
          </Toggle>
          <Toggle
            pressed={selectedType === "chance"}
            onPressedChange={() => handleTypeToggle("chance")}
            size="sm"
            className="flex-1 border border-input"
          >
            <Dices className="size-3" />
            <span className="ml-1 text-xs">Шанс</span>
          </Toggle>
        </div>
      </div>
      <ScrollArea className="flex-1">
        <ErrorBoundary fallbackTitle="Couldn't load blocks">
          <Suspense fallback={<BlockListSkeleton />}>
            <BlockListContent
              filters={filters}
              searchMode={searchMode}
              selectedId={selectedId}
              onSelect={onSelect}
            />
          </Suspense>
        </ErrorBoundary>
      </ScrollArea>
    </div>
  );
}
