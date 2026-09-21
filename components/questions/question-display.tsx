"use client"

import { useState } from "react"
import { Edit, Trash2, Clock, Check, X, BookOpen } from "lucide-react"
import Link from "next/link"
import { useMutation, useQueryClient } from "@tanstack/react-query"

import { Button } from "@/components/ui/button"
import { NestedAnswers } from "./nested-answers"
import { CategoryBadge } from "./ui/category-badge"
import { toast } from "sonner"
import type { Answer, Question } from "@/types/questions"
import { deleteQuestion, updateQuestion } from "@/server/questions/question"
import { getRelativeTime } from "@/lib/utils"
import { useUser } from "@/lib/contexts/user-context"
import { Descendant } from "slate"
import { parseMarkdownToSlate, serializeToMarkdown } from "@/lib/editor-utils"
import { CATEGORIES } from "@/types/questions"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { MarkdownRenderer } from "../lore/questions/question-markdown-renderer"
import { SlateEditorSimple } from "../lore/questions/slate-editor-simple"
import CustomLink from "../no-prefetch-link"

interface QuestionDisplayProps {
  question: Question
  /** Root-level answers, pre-populated by the parent via the unified getQuestionById call. */
  prefetchedAnswers?: Answer[]
  /** IDs of ancestor answers whose replies are already inlined (for auto-expand). */
  expandedIds?: string[]
  /** The answer ID from the URL (?answerId=) to scroll-to and highlight. */
  targetAnswerId?: string | null
  onQuoteClick?: (quoteText: string) => void
  onQuestionUpdate?: (updatedQuestion: Partial<Question>) => void
}

export function QuestionDisplay({
  question,
  prefetchedAnswers,
  expandedIds = [],
  targetAnswerId,
  onQuoteClick,
  onQuestionUpdate,
}: Readonly<QuestionDisplayProps>) {
  const { user } = useUser()
  const queryClient = useQueryClient()
  const isOwner = question.authorId === user.id

  const [isEditing, setIsEditing] = useState(false)
  const [editTitle, setEditTitle] = useState(question.title)
  const [editBody, setEditBody] = useState<Descendant[]>(() =>
    parseMarkdownToSlate(question.body)
  )
  const [editCategory, setEditCategory] = useState(question.category)

  const isLoreType = question.type?.name === "Лор"

  // Answers and expand path come pre-fetched from QALayout via the unified
  // getQuestionById(questionId, answerId?) call — no extra requests needed here.
  const answers = prefetchedAnswers
  const expandPath = new Set<string>(expandedIds)

  const updateMutation = useMutation({
    mutationFn: updateQuestion,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["questions"] })
      toast.success("Вопрос успешно обновлен!")
      setIsEditing(false)
      onQuestionUpdate?.({
        title: editTitle,
        body: serializeToMarkdown(editBody),
        category: editCategory,
        updatedAt: new Date(),
      })
    },
    onError: () => {
      toast.error("Ошибка обновления вопроса, попробуйте снова и напишите мне")
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteQuestion,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["questions"] })
      toast.success("Вопрос удален успешно!")
    },
    onError: () => {
      toast.error("Ошибка удаления вопроса, попробуйте снова и напишите мне")
    },
  })

  const handleDelete = () => {
    if (window.confirm("Точно хочешь это удалить? Ты же уже спросил")) {
      deleteMutation.mutate(question.id)
    }
  }

  const handleUpdate = () => {
    if (!editTitle.trim() || editBody.length === 0) return
    updateMutation.mutate({
      id: question.id,
      title: editTitle.trim(),
      body: serializeToMarkdown(editBody),
      category: isLoreType ? undefined : editCategory,
    })
  }

  const handleCancel = () => {
    setIsEditing(false)
    setEditTitle(question.title)
    setEditBody(parseMarkdownToSlate(question.body))
    setEditCategory(question.category)
  }

  return (
    <div className="h-full overflow-y-auto flex flex-col" style={{ scrollBehavior: "smooth" }}>
      <div className="p-6 flex-1">
        {/* Header */}
        <div className="mb-6 animate-in fade-in-up duration-300">
          {/* Category Badge */}
          {isEditing && !isLoreType ? (
            <Select
              value={editCategory?.id ?? "4"}
              onValueChange={(id) => {
                const selectedCategory = CATEGORIES.find((cat) => cat.id === id)
                if (selectedCategory) setEditCategory(selectedCategory)
              }}
            >
              <SelectTrigger className="w-fit h-8 mb-3">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {CATEGORIES.map((cat) => (
                  <SelectItem key={cat.id} value={cat.id}>
                    <CategoryBadge category={cat} size="sm" />
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          ) : (
            question.category && (
              <div className="mb-3">
                <CategoryBadge category={question.category} size="md" />
              </div>
            )
          )}

          {question.loreId && (
            <CustomLink
              href={`./lore/${question.loreId}`}
              className="inline-flex items-center gap-1.5 text-sm text-blue-400 hover:underline mb-3"
            >
              <BookOpen className="w-4 h-4" />
              Перейти к лору
            </CustomLink>
          )}

          {question.characterId && (
            <CustomLink
              href={`./characters/${question.characterId}`}
              className="inline-flex items-center gap-1.5 text-sm text-blue-400 hover:underline mb-3"
            >
              <BookOpen className="w-4 h-4" />
              Перейти к персонажу
            </CustomLink>
          )}

          {isEditing ? (
            <input
              type="text"
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
              className="w-full px-3 py-2 text-2xl font-bold bg-background border border-input rounded-md focus:outline-none focus:ring-2 focus:ring-ring text-foreground"
            />
          ) : (
            <h1 className="text-2xl font-bold mb-2">{question.title}</h1>
          )}

          {!isEditing && (
            <div className="text-sm text-muted-foreground mb-4 flex items-center space-x-2">
              <span>
                Вопрос от{" "}
                <span className="font-medium">{question.author as string}</span> •{" "}
                {getRelativeTime(question.createdAt)}
              </span>
              {question.updatedAt && (
                <>
                  <span>•</span>
                  <div className="flex items-center space-x-1 text-amber-600">
                    <Clock className="w-3 h-3" />
                    <span>Изменено {getRelativeTime(question.updatedAt)}</span>
                  </div>
                </>
              )}
            </div>
          )}

          {isOwner && !isEditing && (
            <div className="flex space-x-2 animate-in fade-in duration-500 delay-200">
              <Button
                onClick={() => setIsEditing(true)}
                variant="outline"
                className="hover:scale-105 transition-all duration-200 bg-transparent"
              >
                <Edit className="w-4 h-4 mr-2" />
                Изменить
              </Button>
              <Button
                variant="outline"
                onClick={handleDelete}
                disabled={deleteMutation.isPending}
                className="hover:scale-105 transition-all duration-200 hover:bg-red-200 hover:text-red-600 bg-transparent"
              >
                <Trash2 className="w-4 h-4 mr-2" />
                {deleteMutation.isPending ? "Удаляю..." : "Удалить"}
              </Button>
            </div>
          )}

          {isEditing && (
            <div className="flex space-x-2 mt-4">
              <Button
                size="sm"
                onClick={handleUpdate}
                disabled={updateMutation.isPending}
                className="gap-1"
              >
                <Check className="w-3.5 h-3.5" />
                Сохранить
              </Button>
              <Button size="sm" variant="ghost" onClick={handleCancel} className="gap-1">
                <X className="w-3.5 h-3.5" />
                Отмена
              </Button>
            </div>
          )}
        </div>

        {/* Question Body */}
        <div className="prose max-w-none leading-relaxed animate-in fade-in-up duration-300 delay-100 mb-8">
          {isEditing ? (
            <SlateEditorSimple initialContent={editBody} onChange={setEditBody} />
          ) : (
            <MarkdownRenderer
              content={question.body}
              className="text-sm text-foreground/90 prose prose-sm max-w-none prose-p:my-1"
              onQuoteClick={onQuoteClick}
            />
          )}
        </div>

        {/* Answers Section */}
        {answers && answers.length > 0 && (
          <div className="border-t border-input pt-6">
            <h3 className="text-lg font-semibold mb-4">Ответы ({answers.length})</h3>
            <NestedAnswers
              answers={answers}
              questionId={question.id}
              expandPath={expandPath}
              targetAnswerId={targetAnswerId}
              onQuote={onQuoteClick}
            />
          </div>
        )}
      </div>
    </div>
  )
}
