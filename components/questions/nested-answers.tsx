"use client"

import type { Answer } from "@/types/questions"
import { AnswerItem } from "../lore/questions/answer-item"

interface NestedAnswersProps {
  answers: Answer[]
  questionId: string
  /** IDs of answers that should be auto-expanded to reveal targetAnswerId */
  expandPath?: Set<string>
  /** The specific answer id that should be highlighted/targeted */
  targetAnswerId?: string | null
  onQuote?: (text: string) => void
}

export function NestedAnswers({
  answers,
  questionId,
  expandPath,
  targetAnswerId,
  onQuote,
}: Readonly<NestedAnswersProps>) {
  return (
    <div className="space-y-4">
      {answers.map((answer) => (
        <AnswerItem
          key={answer.id}
          answer={answer}
          questionId={questionId}
          depth={0}
          expandPath={expandPath}
          targetAnswerId={targetAnswerId}
          onQuoteClick={onQuote}
        />
      ))}
    </div>
  )
}
