"use client";

import { useState, useEffect, useCallback } from "react";
import { Search, Plus } from "lucide-react";
import { useSearchParams, useRouter, useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { AnimatedToggle } from "./ui/animated-toggle";
import { AuthorDropdown } from "./ui/author-dropdown";
import { CategoryDropdown } from "./ui/category-dropdown";
import { TypeDropdown } from "./ui/type-dropdown";
import { Author, QuestionCategory, QuestionType } from "@/types/questions";
import { getRoomAuthors } from "@/server/questions/question";
import { useRoomId } from "@/lib/room-utils";

interface FilterControlsProps {
  onCreateQuestion: () => void;
}

function useDebounce(value: string, delay: number) {
  const [debouncedValue, setDebouncedValue] = useState(value);
  useEffect(() => {
    const handler = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(handler);
  }, [value, delay]);
  return debouncedValue;
}

export function FilterControls({
  onCreateQuestion,
}: Readonly<FilterControlsProps>) {
  const searchParams = useSearchParams();
  const router = useRouter();
  const roomName = useRoomId();

  // ── Authors ──────────────────────────────────────────────────────────────
  const { data: authors = [] } = useQuery({
    queryKey: ["roomAuthors", roomName],
    queryFn: () => getRoomAuthors(roomName),
    enabled: !!roomName,
    staleTime: 24 * 60 * 60 * 1000,
    gcTime: 24 * 60 * 60 * 1000,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    retry: false,
  });

  // ── Read filter values from URL ───────────────────────────────────────────
  const searchQuery = searchParams.get("search") ?? "";
  const statusParam = searchParams.get("status");
  const statusFilter: boolean | null =
    statusParam === "answered"
      ? true
      : statusParam === "unanswered"
        ? false
        : null;
  const myQuestionsOnly = searchParams.get("mine") === "1";

  const authorId = searchParams.get("authorId");
  const authorName = searchParams.get("authorName");
  const authorFilter: Author | null =
    authorId && authorName ? { id: authorId, name: authorName } : null;

  const categoryId = searchParams.get("categoryId");
  const categoryName = searchParams.get("categoryName");
  const categoryFilter: QuestionCategory | null =
    categoryId && categoryName
      ? { id: categoryId, name: categoryName as QuestionCategory["name"] }
      : null;

  const typeId = searchParams.get("typeId");
  const typeName = searchParams.get("typeName");
  const typeFilter: QuestionType | null =
    typeId && typeName
      ? { id: typeId, name: typeName as QuestionType["name"] }
      : null;

  // ── Write helpers ─────────────────────────────────────────────────────────
  const setParam = useCallback(
    (updates: Record<string, string | null>) => {
      const params = new URLSearchParams(searchParams.toString());
      for (const [key, value] of Object.entries(updates)) {
        if (value === null || value === "") params.delete(key);
        else params.set(key, value);
      }
      router.push(`?${params.toString()}`, { scroll: false });
    },
    [searchParams, router],
  );

  // ── Search (debounced) ────────────────────────────────────────────────────
  const [localSearch, setLocalSearch] = useState(searchQuery);
  const [searchFocused, setSearchFocused] = useState(false);

  // Keep local value in sync when URL changes externally (back/forward nav)
  useEffect(() => {
    setLocalSearch(searchQuery);
  }, [searchQuery]);

  const debouncedSearch = useDebounce(localSearch, 300);
  useEffect(() => {
    setParam({ search: debouncedSearch || null });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedSearch]);

  // ── Filter handlers ───────────────────────────────────────────────────────
  const handleStatusChange = (s: boolean | null) =>
    setParam({
      status: s === true ? "answered" : s === false ? "unanswered" : null,
    });

  const handleMyQuestionsToggle = (enabled: boolean) => {
    if (enabled) {
      setParam({ mine: "1", authorId: null, authorName: null });
    } else {
      setParam({ mine: null });
    }
  };

  const handleAuthorChange = (author: Author) => {
    if (author.id === "-1") setParam({ authorId: null, authorName: null });
    else setParam({ authorId: author.id, authorName: author.name });
  };

  const handleCategoryChange = (cat: QuestionCategory | null) =>
    setParam(
      cat
        ? { categoryId: cat.id, categoryName: cat.name }
        : { categoryId: null, categoryName: null },
    );

  const handleTypeChange = (type: QuestionType | null) =>
    setParam(
      type
        ? { typeId: type.id, typeName: type.name }
        : { typeId: null, typeName: null },
    );

  return (
    <div className="space-y-3">
      {/* Create Question Button */}
      <Button
        onClick={onCreateQuestion}
        className="w-full bg-primary hover:bg-primary/90 transform transition-all duration-200"
      >
        <Plus className="w-4 h-4 mr-2" />
        Создать вопрос
      </Button>

      {/* Search Input */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 transition-colors duration-200 text-muted-foreground" />
        <Input
          placeholder="Поиск по заголовкам"
          value={localSearch}
          onChange={(e) => setLocalSearch(e.target.value)}
          onFocus={() => setSearchFocused(true)}
          onBlur={() => setSearchFocused(false)}
          className={`pl-10 transition-all duration-200 ${
            searchFocused ? "ring-2 ring-input border-input" : ""
          }`}
        />
      </div>

      {/* Status Filter */}
      <div className="flex space-x-1 justify-evenly">
        <Button
          variant={statusFilter === null ? "default" : "ghost"}
          size="sm"
          onClick={() => handleStatusChange(null)}
          className="transition-all duration-200"
        >
          Все
        </Button>
        <Button
          variant={statusFilter === true ? "default" : "ghost"}
          size="sm"
          onClick={() => handleStatusChange(true)}
          className={`transition-all duration-200 ${
            statusFilter === true
              ? "bg-green-500 hover:bg-green-600"
              : "hover:bg-green-200 hover:text-green-600"
          }`}
        >
          С ответом
        </Button>
        <Button
          variant={statusFilter === false ? "default" : "ghost"}
          size="sm"
          onClick={() => handleStatusChange(false)}
          className={`transition-all duration-200 ${
            statusFilter === false
              ? "bg-red-500 hover:bg-red-600"
              : "hover:bg-red-200 hover:text-red-600"
          }`}
        >
          Без ответа
        </Button>

        <AnimatedToggle
          enabled={myQuestionsOnly}
          onToggle={handleMyQuestionsToggle}
          label="Мои вопросы"
        />
      </div>

      {/* Author Dropdown — disabled when My Questions is active */}
      <AuthorDropdown
        selectedAuthor={authorFilter}
        onAuthorChange={handleAuthorChange}
        disabled={myQuestionsOnly}
        authors={authors}
      />

      {/* Category Dropdown */}
      <CategoryDropdown
        selectedCategory={categoryFilter}
        onCategoryChange={handleCategoryChange}
        disabled={myQuestionsOnly}
      />

      {/* Type Dropdown */}
      <TypeDropdown
        selectedType={typeFilter}
        onTypeChange={handleTypeChange}
        disabled={myQuestionsOnly}
      />
    </div>
  );
}
