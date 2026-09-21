"use client"

import type { Question } from "@/types/questions"
import { CheckCircle2, Circle } from "lucide-react"
import { MarkdownRenderer } from "./question-markdown-renderer"

interface QuestionListItemProps {
  question: Question
  onClick: () => void
  onQuoteClick?: (quoteText: string) => void
}

export function QuestionListItem({
  question,
  onClick,
  onQuoteClick,
}: Readonly<QuestionListItemProps>) {
  const formattedDate = new Date(question.createdAt).toLocaleDateString(
    "ru-RU",
    { day: "numeric", month: "short" }
  )

  return (
    <button
      onClick={onClick}
      className="w-full text-left px-4 py-3 hover:bg-accent/50 transition-colors border-b border-input last:border-b-0 group"
    >
      <div className="flex items-start gap-3">
        <div className="mt-0.5 shrink-0">
          {question.isAnswered ? (
            <CheckCircle2 className="w-4 h-4 text-emerald-500" />
          ) : (
            <Circle className="w-4 h-4 text-muted-foreground/50" />
          )}
        </div>
        <div className="flex-1 min-w-0">
          <h4 className="text-sm font-medium text-foreground group-hover:text-primary transition-colors line-clamp-1">
            {question.title}
          </h4>
          <div className="mt-1 line-clamp-2">
            <MarkdownRenderer
              content={question.body}
              className="text-xs text-muted-foreground prose-p:my-0 [&_p]:text-xs [&_.lore-quote]:pointer-events-none"
              onQuoteClick={onQuoteClick}
            />
          </div>
          <div className="flex items-center gap-2 mt-1.5">
            <span className="text-[10px] text-muted-foreground">
              {question.author}
            </span>
            <span className="text-[10px] text-muted-foreground">
              {formattedDate}
            </span>
            {question.type.name !== "Общие" && (
              <span className="text-[10px] px-1 py-0.5 rounded bg-primary/10 text-primary font-medium">
                {question.type.name}
              </span>
            )}
            {question.category && (
              <span className="text-[10px] px-1 py-0.5 rounded bg-secondary text-secondary-foreground font-medium">
                {question.category.name}
              </span>
            )}
          </div>
        </div>
      </div>
    </button>
  )
}
