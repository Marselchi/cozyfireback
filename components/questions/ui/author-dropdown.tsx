"use client"

import { useState } from "react"
import { ChevronDown, User } from "lucide-react"
import { Button } from "@/components/ui/button"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { Author } from "@/types/questions"

interface AuthorDropdownProps {
  selectedAuthor: Author | null
  onAuthorChange: (author: Author) => void
  authors: {id: string, name: string}[]
  disabled?: boolean
}


export function AuthorDropdown({ selectedAuthor, onAuthorChange, disabled = false, authors }: AuthorDropdownProps) {
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
            <User className="w-4 h-4 mr-2" />
            {selectedAuthor == null ? "Все авторы" : selectedAuthor.name}
          </div>
          <ChevronDown className={`w-4 h-4 transition-transform duration-200 ${isOpen ? "rotate-180" : ""}`} />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-full animate-in slide-in-from-top-2 duration-200" align="start">
        <DropdownMenuItem
            key='-1'
            onClick={() => onAuthorChange({id: "-1", name: "Все авторы"})}
            className="cursor-pointer transition-colors duration-150"
          >
            Все авторы
          </DropdownMenuItem>
        {authors.map((author) => (
          <DropdownMenuItem
            key={author.id}
            onClick={() => onAuthorChange(author)}
            className="cursor-pointer transition-colors duration-150"
          >
            {author.name}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
