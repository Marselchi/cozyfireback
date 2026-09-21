"use client"

import { useState } from "react"
import { ChevronDown, Tag } from "lucide-react"
import { Button } from "@/components/ui/button"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { CategoryBadge } from "./category-badge"
import { CATEGORIES, QuestionType, type QuestionCategory } from "@/types/questions"

interface CategoryDropdownProps {
  selectedCategory: QuestionCategory | null
  onCategoryChange: (category: QuestionCategory | null) => void
  disabled?: boolean
}

const categories: (QuestionCategory | null)[] = [
  null,
  { id: "0", name: "Мир" },
  { id: "1", name: "Лор" },
  { id: "2", name: "Предметы" },
  { id: "3", name: "Правила" },
  { id: "4", name: "Общее" },
  { id: "5", name: "Персонажи" },
  { id: "6", name: "Мета" },
]

export function CategoryDropdown({ selectedCategory, onCategoryChange, disabled = false }: CategoryDropdownProps) {
  const [isOpen, setIsOpen] = useState(false)

  return (
    <DropdownMenu open={isOpen} onOpenChange={setIsOpen}>
      <DropdownMenuTrigger asChild>
        <Button
          variant="outline"
          size="sm"
          disabled={disabled}
          className={`w-full justify-between transition-all duration-200 bg-transparent ${
            disabled ? "opacity-50 cursor-not-allowed" : "hover:bg-accent"
          }`}
        >
          <div className="flex items-center">
            <Tag className="w-4 h-4 mr-2" />
            {selectedCategory ? <CategoryBadge category={selectedCategory} size="sm" /> : "Все категории"}
          </div>
          <ChevronDown className={`w-4 h-4 transition-transform duration-200 ${isOpen ? "rotate-180" : ""}`} />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-full animate-in slide-in-from-top-2 duration-200" align="start">
        {categories.map((category) => (
          <DropdownMenuItem
            key={category ? category.id : "ohh"}
            onClick={() => onCategoryChange(category != null ? (category as QuestionCategory) : null)}
            className="cursor-pointer  transition-colors duration-150"
          >
            {category === null ? (
              "Все категории"
            ) : (
              <CategoryBadge category={category as QuestionCategory} size="sm" />
            )}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
