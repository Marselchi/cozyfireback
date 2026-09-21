"use client"

import { useState } from "react"
import { ChevronDown, Tag } from "lucide-react"
import { Button } from "@/components/ui/button"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { CategoryBadge } from "./category-badge"
import { CATEGORIES, QuestionType, type QuestionCategory } from "@/types/questions"
import { TypeBadge } from "./type-badge"

interface TypeDropdownProps {
  selectedType: QuestionType | null
  onTypeChange: (category: QuestionType | null) => void
  disabled?: boolean
}

const types: (QuestionType | null)[] = [
  null,
  {id: "0", name: "Общие"},
  {id: "1", name: "Лор"},
  {id: "2", name: "Персонаж"}
]


export function TypeDropdown({ selectedType, onTypeChange, disabled = false }: Readonly<TypeDropdownProps>) {
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
            {selectedType ? <TypeBadge type={selectedType} size="sm" /> : "Все типы"}
          </div>
          <ChevronDown className={`w-4 h-4 transition-transform duration-200 ${isOpen ? "rotate-180" : ""}`} />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-full animate-in slide-in-from-top-2 duration-200" align="start">
        {types.map((type) => (
          <DropdownMenuItem
            key={type ? type.id : "ohh"}
            onClick={() => onTypeChange(type != null ? (type as QuestionType) : null)}
            className="cursor-pointer  transition-colors duration-150"
          >
            {type === null ? (
              "Все типы"
            ) : (
              <TypeBadge   type={type} size="sm" />
            )}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
