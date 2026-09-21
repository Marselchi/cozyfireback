"use client";

import { useCallback, useEffect, useRef } from "react";
import { useInfiniteQuery } from "@tanstack/react-query";
import { QuestionCard } from "./question-card";
import { QuestionCardSkeleton } from "./ui/question-card-skeleton";
import {
  QuestionCategory,
  Question,
  Author,
  QuestionType,
} from "@/types/questions";
import { PaginatedQuestionsResult } from "@/server/questions/question";
import { Loader2 } from "lucide-react";
import { useUser } from "@/lib/contexts/user-context";

interface QuestionListProps {
  roomName: string;
  searchQuery: string;
  statusFilter: boolean | null;
  myQuestionsOnly: boolean;
  authorFilter: Author | null;
  categoryFilter: QuestionCategory | null;
  typeFilter: QuestionType | null;
  onQuestionSelect: (question: Question) => void;
  selectedQuestionId: string | null;
}

type QuestionsInfiniteData = {
  pages: PaginatedQuestionsResult[];
  pageParams: number[];
  allQuestions: Question[];
};

export function QuestionList({
  roomName,
  searchQuery,
  statusFilter,
  authorFilter,
  categoryFilter,
  typeFilter,
  myQuestionsOnly,
  onQuestionSelect,
  selectedQuestionId,
}: Readonly<QuestionListProps>) {
  const loaderRef = useRef<HTMLDivElement>(null);
  const { user } = useUser();

  let authorId: number | null = null;
  if (myQuestionsOnly) {
    authorId = Number.parseInt(user.id);
  } else if (authorFilter?.id && authorFilter.id !== "-1") {
    authorId = Number.parseInt(authorFilter.id);
  }

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    isError,
  } = useInfiniteQuery<PaginatedQuestionsResult, Error, QuestionsInfiniteData>({
    queryKey: [
      "questions",
      roomName,
      searchQuery,
      statusFilter,
      authorFilter?.id ?? null,
      categoryFilter?.id ?? null,
      typeFilter?.id ?? null,
      myQuestionsOnly,
    ],
    enabled: !!roomName,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    retry: false,
    queryFn: async () => {

      // Формируем тело запроса так же, как вы передавали аргументы в getFilterQuestions
      const requestBody = {
        filters: {
          search: searchQuery ?? " ",
          isAnswered: statusFilter,
          authorId,
          categoryId: categoryFilter?.id
            ? Number.parseInt(categoryFilter.id)
            : null,
          loreId: null,
          characterId: null,
          hasLore: typeFilter !== null ? typeFilter.id === "1" : null,
          hasCharacter: typeFilter !== null ? typeFilter.id === "2" : null,
        },
        pageable: { page: 0, size: 20 },
        roomName: roomName,
      };

      // Делаем запрос к вашему API роуту
      // Замените '/api/questions/filter' на реальный путь к вашему файлу в папке app/api/...
      const response = await fetch("/api/questions/list", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(requestBody),
      });

      if (!response.ok) {
        throw new Error("Failed to fetch questions");
      }

      const data = await response.json();

      return data;
    },
    initialPageParam: 0,
    getNextPageParam: (lastPage) => {
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

  if (isLoading) {
    return (
      <div className="h-full overflow-y-auto p-4 space-y-3">
        {Array.from({ length: 10 }).map((_, i) => (
          <QuestionCardSkeleton key={i} />
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <div className="h-full overflow-y-scroll p-4">
        <div className="text-center text-red-500">
          Ошибка загрузки вопросов. Попробуйте еще раз и напишите мне
        </div>
      </div>
    );
  }

  const questions = data?.allQuestions ?? [];

  if (!questions.length) {
    return (
      <div className="h-full flex items-center justify-center mt-4">
        <div className="text-center text-muted-foreground">
          <p className="text-sm">Нет вопросов подходящих под ваш фильтр</p>
        </div>
      </div>
    );
  }

  return (
    <div
      className="h-full overflow-y-auto p-4 space-y-3 question-list"
      style={{ scrollBehavior: "smooth" }}
    >
      {questions.map((question, index) => (
        <QuestionCard
          key={question.id}
          question={question}
          isSelected={selectedQuestionId === question.id}
          onClick={() => onQuestionSelect(question)}
          animationDelay={index * 50}
        />
      ))}

      {/* Sentinel for infinite scroll */}
      <div ref={loaderRef} className="py-4 flex justify-center">
        {hasNextPage && (
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
        {!hasNextPage && questions.length > 0 && (
          <span className="text-xs text-muted-foreground/70">
            Все вопросы загружены
          </span>
        )}
      </div>
    </div>
  );
}
