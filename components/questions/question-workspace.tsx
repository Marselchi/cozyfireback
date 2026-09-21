"use client"

import { QuestionDisplay } from "./question-display"
import { RichTextEditor } from "./rich-text-editor"
import { QuestionCardSkeleton } from "./ui/question-card-skeleton"
import type { Answer, Question } from "@/types/questions"

interface QuestionWorkspaceProps {
  selectedQuestion: Question | null
  isLoadingQuestion?: boolean
  targetAnswerId?: string | null
  prefetchedAnswers?: Answer[]
  expandedIds?: string[]
  onQuestionUpdate?: (updatedQuestion: Partial<Question>) => void
}

export function QuestionWorkspace({
  selectedQuestion,
  isLoadingQuestion,
  targetAnswerId,
  prefetchedAnswers,
  expandedIds,
  onQuestionUpdate,
}: Readonly<QuestionWorkspaceProps>) {
  const handleReplySuccess = () => {
    onQuestionUpdate?.({})
  }

  if (isLoadingQuestion) {
    return (
      <div className="flex-1 p-4 space-y-3" data-workspace="true">
        <QuestionCardSkeleton />
        <QuestionCardSkeleton />
        <QuestionCardSkeleton />
      </div>
    )
  }

  if (!selectedQuestion) {
    return (
      <div className="flex-1 flex items-center justify-center text-gray-500" data-workspace="true">
        <div className="text-center animate-in fade-in duration-500">
          <h3 className="text-lg font-medium mb-2">Выберите вопрос</h3>
          <p className="text-sm">Выберите вопрос слева чтобы просмотреть и ответить</p>
        </div>
      </div>
    )
  }

  return (
    <div className="flex-1 flex flex-col" data-workspace="true">
      {/* Question Display */}
      <div className="flex-1 overflow-hidden">
        <div className="animate-in fade-in-up duration-300">
          <QuestionDisplay
            question={selectedQuestion}
            targetAnswerId={targetAnswerId}
            prefetchedAnswers={prefetchedAnswers}
            expandedIds={expandedIds}
            onQuestionUpdate={onQuestionUpdate}
          />
        </div>
      </div>

      {/* Editor Section */}
      <RichTextEditor question={selectedQuestion} onReplySuccess={handleReplySuccess} />
    </div>
  )
}
