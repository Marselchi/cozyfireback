"use client";

import { useSearchParams, useRouter, useParams } from "next/navigation";
import { useCallback, useEffect } from "react";
import { QuestionBrowser } from "./question-browser";
import { QuestionWorkspace } from "./question-workspace";
import type { Question, QuestionWithAnswers } from "@/types/questions";
import { useQueryClient, useQuery } from "@tanstack/react-query";
import { useRoomId } from "@/lib/room-utils";

export default function QALayout() {
  const queryClient = useQueryClient();
  const searchParams = useSearchParams();
  const router = useRouter();
  const roomName = useRoomId();

  const questionId = searchParams.get("questionId");
  const answerId = searchParams.get("answerId");

  // getQuestionById(questionId, answerId?) returns QuestionWithAnswers:
  //   - always includes root-level answers
  //   - when answerId is present, ancestor replies are pre-populated and expandedIds is populated
  const { data: fetchedData, isLoading: questionLoading } =
    useQuery<QuestionWithAnswers | null>({
      queryKey: ["question", questionId, answerId],
      queryFn: async () => {
        if (!questionId) throw new Error("questionId is required");

        const params = new URLSearchParams({
          roomName: roomName,
        });

        if (answerId !== null && answerId !== undefined) {
          params.append("answerId", answerId.toString());
        }

        const response = await fetch(
          `/api/questions/${questionId}?${params.toString()}`,
          {
            method: "GET",
            headers: {
              "Content-Type": "application/json",
            },
          },
        );

        if (!response.ok) {
          throw new Error("Failed to fetch question");
        }

        return response.json();
      },
      enabled: !!questionId,
      staleTime: 5 * 60 * 1000,
    });


  // Prefer the cached list version for question metadata so edits in the sidebar
  // are immediately reflected without waiting for a refetch.
  const selectedQuestion: Question | null = fetchedData?.question ?? null;

  const answers = fetchedData?.answers;
  const expandedIds = fetchedData?.expandedIds ?? [];
  const isLoadingQuestion =
    !!questionId && questionLoading && !selectedQuestion;

  const handleQuestionSelect = useCallback(
    (question: Question) => {
      const params = new URLSearchParams(searchParams.toString());
      params.set("questionId", question.id);
      params.delete("answerId");
      router.push(`?${params.toString()}`, { scroll: false });
    },
    [router, searchParams],
  );

  const handleQuestionUpdate = useCallback(
    (updatedQuestion: Partial<Question>) => {
      if (!questionId) return;
      // Update the unified cache entry
      queryClient.setQueryData<QuestionWithAnswers | null>(
        ["question", questionId, answerId],
        (old) =>
          old
            ? { ...old, question: { ...old.question, ...updatedQuestion } }
            : old,
      );
      // Also invalidate the list so the sidebar reflects the change
      queryClient.invalidateQueries({ queryKey: ["questions"] });
    },
    [questionId, answerId, queryClient],
  );

  return (
    <div className="flex max-h-full overflow-y-auto">
      {/* Left Column - Question Browser (30%) */}
      <div className="w-[30%] border-r border-input flex flex-col sticky top-0 max-h-vh">
        <QuestionBrowser
          onQuestionSelect={handleQuestionSelect}
          selectedQuestionId={questionId}
        />
      </div>

      {/* Right Column - Workspace (70%) */}
      <div className="w-[70%] flex flex-col h-screen">
        <QuestionWorkspace
          selectedQuestion={selectedQuestion}
          isLoadingQuestion={isLoadingQuestion}
          prefetchedAnswers={answers}
          expandedIds={expandedIds}
          targetAnswerId={answerId}
          onQuestionUpdate={handleQuestionUpdate}
        />
      </div>
    </div>
  );
}
