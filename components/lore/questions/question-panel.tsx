"use client";

import { Suspense, useCallback, useEffect, useRef, useState } from "react";
import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import {
  Plus,
  Search,
  Filter,
  X,
  MessageSquareText,
  Loader2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { QuestionListItem } from "./question-list-item";
import { QuestionDetail } from "./question-detail";
import { CreateQuestionForm } from "./create-question-form";
import type { Question, QuestionsFilters } from "@/types/questions";
import { Skeleton } from "@/components/ui/skeleton";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  getFilterQuestions,
  PaginatedQuestionsResult,
} from "@/server/questions/question";
import { useParams } from "next/navigation";
import { useRoomId } from "@/lib/room-utils";

interface QuestionPanelProps {
  loreId?: string;
  characterId?: string;
  onQuoteClick?: (quoteText: string) => void;
}

type FilterStatus = "all" | "answered" | "unanswered";

type QuestionsInfiniteData = {
  pages: PaginatedQuestionsResult[];
  pageParams: number[];
  allQuestions: Question[];
};

function QuestionsListContent({
  roomName,
  filters,
  pageSize = 20,
  loreId,
  characterId,
  onSelectQuestion,
  onQuoteClick,
}: Readonly<{
  roomName: string;
  filters: QuestionsFilters;
  pageSize?: number;
  loreId?: string;
  characterId?: string;
  onSelectQuestion: (q: Question) => void;
  onQuoteClick?: (quoteText: string) => void;
}>) {
  const loaderRef = useRef<HTMLDivElement>(null);
  const [hasMore, setHasMore] = useState(true);
  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    refetch,
  } = useInfiniteQuery<PaginatedQuestionsResult, Error, QuestionsInfiniteData>({
    queryKey: ["questions", filters, loreId, characterId, pageSize],
    queryFn: ({ pageParam = 0 }) =>
      getFilterQuestions({
        filters,
        pageable: { page: pageParam as number, size: pageSize },
        roomName,
        loreId,
        characterId,
      }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => {
      // ← Безопасная проверка: lastPage.page всегда есть по контракту, но TS перестраховывается
      if (!lastPage?.page) return undefined;
      const nextPage = lastPage.page.number + 1;
      return nextPage < lastPage.page.totalPages ? nextPage : undefined;
    },
    select: (data): QuestionsInfiniteData => ({
      pages: data.pages,
      pageParams: data.pageParams as any,
      allQuestions: data.pages.flatMap((page) => page.content ?? []),
    }),
  });

  const handleObserver = useCallback(
    (entries: IntersectionObserverEntry[]) => {
      const target = entries[0];
      if (target.isIntersecting && hasNextPage && !isFetchingNextPage) {
        fetchNextPage();
      }
    },
    [hasNextPage, isFetchingNextPage, fetchNextPage],
  );

  useEffect(() => {
    const element = loaderRef.current;
    if (!element) return;

    const observer = new IntersectionObserver(handleObserver, {
      root: null,
      rootMargin: "100px",
      threshold: 0.1,
    });

    observer.observe(element);
    return () => observer.disconnect();
  }, [handleObserver]);

  useEffect(() => {
    refetch();
    setHasMore(true);
  }, [filters, loreId, characterId, refetch]);

  if (isLoading) {
    return (
      <div className="space-y-1 p-2">
        {[1, 2, 3, 4, 5].map((i) => (
          <div key={i} className="px-4 py-3 space-y-2">
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-3 w-full" />
            <Skeleton className="h-3 w-1/2" />
          </div>
        ))}
      </div>
    );
  }

  const questions = data?.allQuestions ?? [];

  if (!questions.length) {
    return (
      <div className="flex flex-col items-center justify-center py-12 px-4 text-center">
        <MessageSquareText className="w-10 h-10 text-muted-foreground/30 mb-3" />
        <p className="text-sm text-muted-foreground">Вопросов не найдено</p>
        <p className="text-xs text-muted-foreground/70 mt-1">
          Создайте первый вопрос
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col min-h-full">
      <div className="flex-1">
        {questions.map((question) => (
          <QuestionListItem
            key={question.id}
            question={question}
            onClick={() => onSelectQuestion(question)}
            onQuoteClick={onQuoteClick}
          />
        ))}
      </div>

      {/* Sentinel для infinite scroll */}
      <div ref={loaderRef} className="py-4 flex justify-center">
        {hasMore && (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            {isFetchingNextPage ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Загрузка...
              </>
            ) : (
              <span>Прокрутите для загрузки ещё</span>
            )}
          </div>
        )}
        {!hasMore && questions.length > 0 && (
          <span className="text-xs text-muted-foreground/70">
            Все вопросы загружены
          </span>
        )}
      </div>
    </div>
  );
}

export function QuestionPanel({
  loreId,
  characterId,
  onQuoteClick,
}: Readonly<QuestionPanelProps>) {
  const [selectedQuestion, setSelectedQuestion] = useState<Question | null>(
    null,
  );
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [filterStatus, setFilterStatus] = useState<FilterStatus>("all");
  const [showFilters, setShowFilters] = useState(false);

  const { id } = useParams<{ id: string }>();
  const roomName = useRoomId();

  const filters: QuestionsFilters = {
    search: searchQuery,
    isAnswered:
      filterStatus === "answered"
        ? true
        : filterStatus === "unanswered"
          ? false
          : null,
    authorId: null,
    categoryId: null,
    hasLore: loreId ? true : null,
    hasCharacter: characterId ? true : null,
    loreId: loreId ? Number.parseInt(id) : null,
    characterId: characterId ? Number.parseInt(id) : null,
  };

  // If viewing a question detail
  if (selectedQuestion) {
    return (
      <div className="flex flex-col h-full">
        <QuestionDetail
          question={selectedQuestion}
          onBack={() => setSelectedQuestion(null)}
          onQuoteClick={onQuoteClick}
        />
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full ">
      {/* Panel header */}
      <div className="shrink-0 p-3 border-b border-border space-y-3">
        {/* Search */}
        <div className="flex items-center gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-muted-foreground" />
            <Input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Поиск..."
              className="h-8 pl-8 text-xs"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery("")}
                className="absolute right-2 top-1/2 -translate-y-1/2"
              >
                <X className="w-3 h-3 text-muted-foreground hover:text-foreground" />
              </button>
            )}
          </div>
          <Button
            variant={showFilters ? "secondary" : "outline"}
            size="sm"
            className="h-8 w-8 p-0"
            onClick={() => setShowFilters(!showFilters)}
          >
            <Filter className="w-3.5 h-3.5" />
          </Button>
          <Button
            size="sm"
            className="h-8 gap-1 text-xs"
            onClick={() => setShowCreateForm(true)}
          >
            <Plus className="w-3.5 h-3.5" />
          </Button>
        </div>

        {/* Filter pills */}
        {showFilters && (
          <div className="flex items-center gap-1.5">
            {(["all", "answered", "unanswered"] as const).map((status) => (
              <button
                key={status}
                onClick={() => setFilterStatus(status)}
                className={`px-2.5 py-1 rounded-full text-[11px] font-medium transition-colors ${
                  filterStatus === status
                    ? "bg-primary text-primary-foreground"
                    : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                }`}
              >
                {status === "all"
                  ? "Все"
                  : status === "answered"
                    ? "С ответом"
                    : "Без ответа"}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Create form */}
      {showCreateForm && (
        <div className="shrink-0 border-b border-border">
          <CreateQuestionForm
            loreId={loreId}
            characterId={characterId}
            onClose={() => setShowCreateForm(false)}
          />
        </div>
      )}

      {/* Questions list */}
      <ScrollArea className="flex-1">
        <Suspense
          fallback={
            <div className="space-y-1 p-2">
              {[1, 2, 3].map((i) => (
                <div key={i} className="px-4 py-3 space-y-2">
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="h-3 w-full" />
                </div>
              ))}
            </div>
          }
        >
          <QuestionsListContent
            roomName={roomName}
            filters={filters}
            loreId={loreId}
            characterId={characterId}
            onSelectQuestion={setSelectedQuestion}
            onQuoteClick={onQuoteClick}
            pageSize={20}
          />
        </Suspense>
      </ScrollArea>
    </div>
  );
}
