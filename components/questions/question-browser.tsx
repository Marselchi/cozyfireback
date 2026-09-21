"use client";

import { useState } from "react";
import { useSearchParams, useParams } from "next/navigation";
import { FilterControls } from "./filter-controls";
import { QuestionList } from "./question-list";
import { CreateQuestionModal } from "./create-question-modal";
import {
  Author,
  Question,
  QuestionCategory,
  QuestionType,
} from "@/types/questions";
import { useRoomId } from "@/lib/room-utils";

interface QuestionBrowserProps {
  onQuestionSelect: (question: Question) => void;
  selectedQuestionId: string | null;
}

// Parse typed filter values directly from URL search params.
// Used by QuestionList to know what to fetch.
function useFilters() {
  const searchParams = useSearchParams();

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

  return {
    searchQuery,
    statusFilter,
    myQuestionsOnly,
    authorFilter,
    categoryFilter,
    typeFilter,
  };
}

export function QuestionBrowser({
  onQuestionSelect,
  selectedQuestionId,
}: Readonly<QuestionBrowserProps>) {
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const roomName = useRoomId();
  const {
    searchQuery,
    statusFilter,
    myQuestionsOnly,
    authorFilter,
    categoryFilter,
    typeFilter,
  } = useFilters();

  return (
    <div className="flex flex-col max-h-screen">
      {/* Sticky Header */}
      <div className="border-b border-input p-4 space-y-4 z-20 flex-1">
        <FilterControls onCreateQuestion={() => setCreateModalOpen(true)} />
      </div>

      {/* Question List */}
      <div className="min-h-0">
        <QuestionList
          roomName={roomName}
          searchQuery={searchQuery}
          statusFilter={statusFilter}
          myQuestionsOnly={myQuestionsOnly}
          authorFilter={authorFilter}
          categoryFilter={categoryFilter}
          typeFilter={typeFilter}
          onQuestionSelect={onQuestionSelect}
          selectedQuestionId={selectedQuestionId}
        />
      </div>

      {/* Modals */}
      <CreateQuestionModal
        open={createModalOpen}
        onOpenChange={setCreateModalOpen}
      />
    </div>
  );
}
