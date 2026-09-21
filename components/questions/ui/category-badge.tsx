"use client"

import type { QuestionCategory, QuestionCategoryName } from "@/types/questions"

interface CategoryBadgeProps {
  category: QuestionCategory
  size?: "sm" | "md" | "lg"
}

const categoryColors: Record<QuestionCategoryName, string> = {
  Мир: "bg-green-300 text-green-800 border-green-300",
  Лор: "bg-purple-300 text-purple-800 border-purple-300",
  Предметы: "bg-blue-300 text-blue-800 border-blue-300",
  Правила: "bg-red-300 text-red-800 border-red-300",
  Общее: "bg-gray-300 text-gray-800 border-gray-300",
  Персонажи: "bg-yellow-300 text-yellow-800 border-yellow-300",
  Мета: "bg-indigo-300 text-indigo-800 border-indigo-300",
}

const sizeClasses = {
  sm: "px-2 py-0.5 text-xs",
  md: "px-2.5 py-1 text-sm",
  lg: "px-3 py-1.5 text-base",
}

export function CategoryBadge({ category, size = "sm" }: CategoryBadgeProps) {
  return (
    <span
      className={`
        inline-flex items-center rounded-full border font-medium
        ${categoryColors[category.name]}
        ${sizeClasses[size]}
      `}
    >
      {category.name}
    </span>
  )
}
