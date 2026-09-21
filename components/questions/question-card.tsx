"use client"

import { useState } from "react"
import { CheckCircle, Circle } from "lucide-react"
import { CategoryBadge } from "./ui/category-badge"
import { Question } from "@/types/questions"
import { getRelativeTime } from "@/lib/utils"
import { TypeBadge } from "./ui/type-badge"
import { MarkdownRenderer } from "../lore/questions/question-markdown-renderer"

interface QuestionCardProps {
  question: Question
  isSelected: boolean
  onClick: () => void
  animationDelay: number
}

export function QuestionCard({ question, isSelected, onClick, animationDelay }: Readonly<QuestionCardProps>) {
  const [isHovered, setIsHovered] = useState(false)

  return (
    <div
      className={`
        p-4 rounded-lg border cursor-pointer transition-all duration-200
        ${isSelected ? "border-blue-500 bg-accent shadow-md border-sweep" : "border-input bg-card hover:shadow-lg"}
        ${isHovered ? "transform -translate-y-0.5" : ""}
        animate-in slide-in-from-bottom-4 fade-in
      `}
      style={{ animationDelay: `${animationDelay}ms` }}
      onClick={onClick}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <div className="flex items-start space-x-3">
        {/* Status Indicator */}
        <div className="shrink-0 mt-1">
          {question.isAnswered ? (
            <CheckCircle className="w-5 h-5 text-green-500 pulse-glow" />
          ) : (
            <Circle className="w-5 h-5 text-red-500 throb" />
          )}
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0">
          <div className="mb-2 flex gap-2">
            {question.type && (
                <TypeBadge type={question.type} size="sm"/>
            )}
            {/* Category Badge */}
            {question.category && (
                <CategoryBadge category={question.category} size="sm" />
            )}
          </div>

          <h3 className="font-semibold line-clamp-2 mb-1">{question.title}</h3>
          <div className="text-muted-foreground text-sm mb-2 line-clamp-2">
            <MarkdownRenderer
              content={question.body}
              className="prose prose-sm max-w-none prose-p:my-0 [&_.lore-quote]:pointer-events-none"
            />
          </div>
          <div className="flex items-center text-xs ">
            <span>{question.author}</span>
            <span className="mx-1">•</span>
            <span>{getRelativeTime(question.createdAt)}</span>
          </div>
        </div>
      </div>
    </div>
  )
}
