"use client"

import type { QuestionType, QuestionTypeName } from "@/types/questions"

interface TypeBadgeProps {
  type: QuestionType
  size?: "sm" | "md" | "lg"
}

const typeColors: Record<QuestionTypeName, string> = {
  Общие: "bg-blue-300 text-blue-800 border-2 border-red-500",
  Лор: "bg-purple-300 text-purple-800 border-2 border-red-500",
  Персонаж: "bg-amber-300 text-amber-800 border-2 border-red-500"
}

const sizeClasses = {
  sm: "px-2 py-0.5 text-xs",
  md: "px-2.5 py-1 text-sm",
  lg: "px-3 py-1.5 text-base",
}

export function TypeBadge({ type, size = "sm" }: Readonly<TypeBadgeProps>) {
  return (
    <span
      className={`
        inline-flex items-center rounded-full border font-medium
        ${typeColors[type.name]}
        ${sizeClasses[size]}
      `}
    >
      Тип: {type.name}
    </span>
  )
}
