"use client";

import { useEffect, useMemo, useState, useRef } from "react";
import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { Pin, PinOff, ChevronDown, ChevronUp, EyeOff, Eye } from "lucide-react";

import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import useDebounce from "@/hooks/use-debounce";
import { TextContent } from "../lore/text-content";
import { getLoreByIdUser, getPaginatedLore } from "@/server/lore/lore";
import { useParams } from "next/navigation";
import { LoreList } from "@/types/lore";
import { useRoomId } from "@/lib/room-utils";

export interface TagText {
  id: number;
  name: string;
}

type LoreDetails = LoreList & { content: string };

export function LoreBlock() {
  const roomName = useRoomId();
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebounce(search, 250);
  const [pinnedItems, setPinnedItems] = useState<LoreList[]>([]);
  const [isUIVisible, setIsUIVisible] = useState(true);
  const [isPinnedCollapsed, setIsPinnedCollapsed] = useState(false);
  const loreListQuery = useInfiniteQuery({
    queryKey: ["lore-list", debouncedSearch],
    queryFn: ({ pageParam = 0 }) =>
      getPaginatedLore(
        roomName,
        {
          title: debouncedSearch || undefined,
        },
        {
          page: pageParam,
          size: 6,
        },
      ),
    getNextPageParam: (lastPage) => {
      if (!lastPage.page) {
        return undefined;
      }
      const nextPage = lastPage.page.number + 1;
      if (nextPage >= lastPage.page.totalPages) {
        return undefined;
      }
      return nextPage;
    },
    initialPageParam: 0,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });

  const content: LoreList[] = useMemo(() => {
    return loreListQuery.data?.pages.flatMap((page) => page.content) ?? [];
  }, [loreListQuery.data]);

  const [selectedId, setSelectedId] = useState<number | null>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (
          entries[0].isIntersecting &&
          loreListQuery.hasNextPage &&
          !loreListQuery.isFetchingNextPage
        ) {
          loreListQuery.fetchNextPage();
        }
      },
      { threshold: 1 },
    );

    if (observerTarget.current) {
      observer.observe(observerTarget.current);
    }

    return () => observer.disconnect();
  }, [loreListQuery]);

  const loreDetailsQuery = useQuery({
    queryKey: ["lore", selectedId],
    queryFn: () => getLoreByIdUser(selectedId as number, roomName),
    enabled: selectedId !== null,
    staleTime: 30_000,
  });

  const selectedLore = (loreDetailsQuery.data ?? null) as LoreDetails | null;

  const selectedListItem = useMemo(() => {
    if (selectedId === null) return null;
    return (
      content.find((x) => x.id === selectedId) ??
      pinnedItems.find((x) => x.id === selectedId) ??
      null
    );
  }, [content, pinnedItems, selectedId]);

  const isPinned = (id: number) => pinnedItems.some((x) => x.id === id);

  const togglePin = (item: LoreList) => {
    if (isPinned(item.id)) {
      setPinnedItems(pinnedItems.filter((x) => x.id !== item.id));
    } else {
      setPinnedItems([...pinnedItems, item]);
    }
  };

  const observerTarget = useRef<HTMLDivElement>(null);

  return (
    <div className="h-full min-h-0 flex flex-col gap-3">
      <div className="flex gap-2 mt-2">
        <Button
          variant="outline"
          size="icon"
          onClick={() => setIsUIVisible(!isUIVisible)}
          className="shrink-0 ml-1"
        >
          {isUIVisible ? (
            <EyeOff className="h-4 w-4" />
          ) : (
            <Eye className="h-4 w-4" />
          )}
        </Button>

        {isUIVisible && (
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Поиск по лору..."
            className="flex-1"
          />
        )}
      </div>

      <div
        className={cn(
          "grid min-h-0 flex-1 gap-3",
          isUIVisible ? "grid-cols-[300px_1fr]" : "grid-cols-[1fr]",
        )}
      >
        {isUIVisible && (
          <div className="min-h-0 overflow-auto rounded-md border bg-card">
            {pinnedItems.length > 0 && (
              <div className="border-2 bg-muted/50 ">
                <button
                  onClick={() => setIsPinnedCollapsed(!isPinnedCollapsed)}
                  className="flex w-full items-center justify-between px-3 py-2 text-xs font-semibold text-muted-foreground hover:bg-muted/70 transition-colors"
                >
                  <span>Закреплено ({pinnedItems.length})</span>
                  {isPinnedCollapsed ? (
                    <ChevronDown className="h-3 w-3" />
                  ) : (
                    <ChevronUp className="h-3 w-3" />
                  )}
                </button>

                {!isPinnedCollapsed &&
                  pinnedItems.map((x) => {
                    const active = x.id === selectedId;

                    return (
                      <div
                        key={x.id}
                        className={cn(
                          "flex items-start gap-2 px-3 py-2 transition-colors border",
                          active
                            ? "bg-accent border border-blue-400"
                            : "hover:bg-current/50",
                          x.nonPublic &&
                            "bg-linear-to-br from-amber-300 to-amber-200",
                        )}
                      >
                        <button
                          onClick={() => setSelectedId(x.id)}
                          className="min-w-0 flex-1 text-left"
                        >
                          <div className="flex items-start justify-between gap-2">
                            <div className="min-w-0 flex-1">
                              <div className="truncate text-sm font-medium">
                                {x.title}
                              </div>
                              <div className="mt-1 line-clamp-2 text-xs text-muted-foreground">
                                {x.description}
                              </div>
                              <div className="mt-1 text-xs text-muted-foreground">
                                {x.author}
                                {x.date ? ` • ${x.date}` : ""}
                              </div>
                            </div>

                            {x.byAdmin && (
                              <Badge
                                variant="default"
                                className="shrink-0 text-[10px] px-2 py-0.5 bg-amber-400"
                              >
                                ДМ
                              </Badge>
                            )}
                          </div>

                          {!!x.tags?.length && (
                            <div className="mt-2 flex flex-wrap gap-1">
                              {x.tags.slice(0, 3).map((t) => (
                                <Badge
                                  key={t.id}
                                  variant="secondary"
                                  className="max-w-30 truncate text-[11px]"
                                  title={t.name}
                                >
                                  {t.name}
                                </Badge>
                              ))}
                              {x.tags.length > 3 && (
                                <Badge
                                  variant="outline"
                                  className="text-[11px]"
                                >
                                  +{x.tags.length - 3}
                                </Badge>
                              )}
                            </div>
                          )}
                        </button>

                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 shrink-0"
                          onClick={() => togglePin(x)}
                        >
                          <PinOff className="h-4 w-4 text-muted-foreground" />
                        </Button>
                      </div>
                    );
                  })}
              </div>
            )}

            {loreListQuery.isLoading && (
              <div className="p-3 text-sm text-muted-foreground">
                Загрузка списка...
              </div>
            )}

            {loreListQuery.isError && (
              <div className="p-3 text-sm text-destructive">
                Ошибка загрузки списка
              </div>
            )}

            {!loreListQuery.isLoading && content.length === 0 && (
              <div className="p-3 text-sm text-muted-foreground">
                Ничего не найдено
              </div>
            )}

            {content.map((x) => {
              const active = x.id === selectedId;
              const pinned = isPinned(x.id);

              return (
                <div
                  key={x.id}
                  className={cn(
                    "flex items-start gap-2 border px-3 py-2 transition-colors last:border-b-2",
                    active
                      ? "bg-accent border-blue-400"
                      : "hover:bg-current/50",
                    x.nonPublic &&
                      "bg-linear-to-br from-amber-300 to-amber-200",
                  )}
                >
                  <button
                    onClick={() => setSelectedId(x.id)}
                    className="min-w-0 flex-1 text-left"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0 flex-1">
                        <div className="truncate text-sm font-medium">
                          {x.title}
                        </div>
                        <div className="mt-1 line-clamp-2 text-xs text-muted-foreground">
                          {x.description}
                        </div>
                        <div className="mt-1 text-xs text-muted-foreground">
                          {x.author}
                          {x.date ? ` • ${x.date}` : ""}
                        </div>
                      </div>

                      {x.byAdmin && (
                        <Badge
                          variant="default"
                          className="shrink-0 text-[10px] px-2 py-0.5 bg-amber-400"
                        >
                          ДМ
                        </Badge>
                      )}
                    </div>

                    {!!x.tags?.length && (
                      <div className="mt-2 flex flex-wrap gap-1">
                        {x.tags.slice(0, 3).map((t) => (
                          <Badge
                            key={t.id}
                            variant="secondary"
                            className="max-w-30 truncate text-[11px]"
                            title={t.name}
                          >
                            {t.name}
                          </Badge>
                        ))}
                        {x.tags.length > 3 && (
                          <Badge variant="outline" className="text-[11px]">
                            +{x.tags.length - 3}
                          </Badge>
                        )}
                      </div>
                    )}
                  </button>

                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8 shrink-0"
                    onClick={() => togglePin(x)}
                  >
                    {pinned ? (
                      <PinOff className="h-4 w-4 text-primary" />
                    ) : (
                      <Pin className="h-4 w-4 text-muted-foreground" />
                    )}
                  </Button>
                </div>
              );
            })}

            {loreListQuery.hasNextPage && (
              <div
                ref={observerTarget}
                className="p-3 text-center text-sm text-muted-foreground"
              >
                {loreListQuery.isFetchingNextPage
                  ? "Загрузка..."
                  : "Загрузить еще"}
              </div>
            )}
          </div>
        )}

        <div className="min-h-0 overflow-auto rounded-md border bg-card p-4">
          {!selectedId && (
            <div className="text-sm text-muted-foreground">
              Выбери запись слева
            </div>
          )}

          {selectedId !== null && loreDetailsQuery.isLoading && (
            <div className="text-sm text-muted-foreground">Загрузка...</div>
          )}

          {selectedId !== null && loreDetailsQuery.isError && (
            <div className="text-sm text-destructive">Ошибка загрузки</div>
          )}

          {selectedLore && (
            <>
              <div className="mb-4">
                <div className="text-xl font-semibold leading-tight">
                  {selectedLore.title}
                </div>

                <div className="mt-1 text-xs text-muted-foreground">
                  {selectedLore.author}
                  {selectedLore.date
                    ? ` • ${selectedLore.date}`
                    : selectedListItem?.date
                      ? ` • ${selectedListItem.date}`
                      : ""}
                </div>

                <div className="mt-2 flex flex-wrap gap-1">
                  {selectedLore.byAdmin && (
                    <Badge variant="default" className="text-xs bg-amber-400">
                      ДМ
                    </Badge>
                  )}

                  {selectedLore.tags?.map((t) => (
                    <Badge key={t.id} variant="secondary" className="text-xs">
                      {t.name}
                    </Badge>
                  ))}
                </div>
              </div>
              <TextContent
                content={selectedLore.content ?? ""}
                loreId={selectedLore.id}
                mdOptions={{ anchorClickable: true, showExcerpts: false }}
              />
            </>
          )}
        </div>
      </div>
    </div>
  );
}
