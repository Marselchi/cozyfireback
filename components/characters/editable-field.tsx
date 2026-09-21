"use client"

import { useState, useEffect, useRef, useCallback } from "react"
import { Pencil, Check, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { EditableFieldProps, EditableTextAreaProps } from "@/types/characters"
// Simple inline editable text field with edit button
export function EditableField({
  value,
  isEditing,
  onStartEdit,
  onSave,
  onCancel,
  placeholder = "Не установлено",
  canEdit = true,
  textSize = "text-sm",
}: Readonly<EditableFieldProps>) {
  const [localValue, setLocalValue] = useState(value)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    setLocalValue(value)
  }, [value])

  useEffect(() => {
    if (isEditing && inputRef.current) {
      inputRef.current.focus()
      inputRef.current.select()
    }
  }, [isEditing])

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === "Enter") {
        e.preventDefault()
        onSave(localValue)
      } else if (e.key === "Escape") {
        setLocalValue(value)
        onCancel()
      }
    },
    [localValue, value, onSave, onCancel]
  )

  const handleSave = useCallback(() => {
    onSave(localValue)
  }, [localValue, onSave])

  const handleCancel = useCallback(() => {
    setLocalValue(value)
    onCancel()
  }, [value, onCancel])

  if (isEditing) {
    return (
      <span className="inline-flex items-center gap-1 relative z-10">
        <input
          ref={inputRef}
          type="text"
          value={localValue ?? undefined}
          onChange={(e) => setLocalValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          className={`bg-background border-b border-border px-1 py-0.5 ${textSize} outline-none focus:border-primary min-w-20`}
        />
        <Button
          variant="ghost"
          size="sm"
          className="h-6 w-6 p-0 z-20"
          onClick={handleSave}
          type="button"
          aria-label="Save"
        >
          <Check className="h-3 w-3" />
        </Button>
        <Button
          variant="ghost"
          size="sm"
          className="h-6 w-6 p-0 z-20"
          onClick={handleCancel}
          type="button"
          aria-label="Cancel"
        >
          <X className="h-3 w-3" />
        </Button>
      </span>
    )
  }

  return (
    <span className="inline-flex items-center gap-1 group/field">
      <span className={`px-1 py-0.5 ${textSize}`}>
        {value || <span className="text-muted-foreground italic">{placeholder}</span>}
      </span>
      {canEdit && (
        <Button
          variant="ghost"
          size="sm"
          className="h-6 w-6 p-0 opacity-0 group-hover/field:opacity-100 transition-opacity"
          onClick={onStartEdit}
          aria-label="Edit"
        >
          <Pencil className="h-3 w-3" />
        </Button>
      )}
    </span>
  )
}

// Simple inline editable textarea for longer text with edit button
export function EditableTextArea({
  value,
  isEditing,
  onStartEdit,
  onSave,
  onCancel,
  placeholder = "Нет контента",
  rows = 4,
  canEdit = true,
}: Readonly<EditableTextAreaProps>) {
  const [localValue, setLocalValue] = useState(value)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    setLocalValue(value)
  }, [value])

  useEffect(() => {
    if (isEditing && textareaRef.current) {
      textareaRef.current.focus()
      textareaRef.current.setSelectionRange(
        textareaRef.current.value.length,
        textareaRef.current.value.length
      )
    }
  }, [isEditing])

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === "Escape") {
        setLocalValue(value)
        onCancel()
      }
      // Ctrl/Cmd + Enter to save
      if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) {
        e.preventDefault()
        onSave(localValue)
      }
    },
    [localValue, value, onSave, onCancel]
  )

  const handleSave = useCallback(() => {
    onSave(localValue)
  }, [localValue, onSave])

  const handleCancel = useCallback(() => {
    setLocalValue(value)
    onCancel()
  }, [value, onCancel])

  if (isEditing) {
    return (
      <div className="space-y-2 relative z-10">
        <textarea
          ref={textareaRef}
          value={localValue ?? undefined}
          onChange={(e) => setLocalValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          rows={rows}
          className="w-full bg-background border border-border rounded-md px-2 py-1.5 text-sm outline-none focus:border-primary resize-y min-h-[80px]"
        />
        <div className="flex items-center justify-between">
          <p className="text-xs text-muted-foreground">
            Ctrl+Enter чтобы сохранить, Escape для отмены
          </p>
          <div className="flex items-center gap-1 relative z-20">
            <Button
              variant="outline"
              size="sm"
              className="h-7"
              onClick={handleCancel}
              type="button"
            >
              Отмена
            </Button>
            <Button
              size="sm"
              className="h-7"
              onClick={handleSave}
              type="button"
            >
              Сохранить
            </Button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="group/field relative">
      <div className="px-2 py-1.5 min-h-[40px]">
        {value ? (
          <span className="whitespace-pre-wrap">{value}</span>
        ) : (
          <span className="text-muted-foreground italic">{placeholder}</span>
        )}
      </div>
      {canEdit && (
        <Button
          variant="ghost"
          size="sm"
          className="absolute top-0 right-0 h-7 w-7 p-0 opacity-0 group-hover/field:opacity-100 transition-opacity"
          onClick={onStartEdit}
          aria-label="Edit"
        >
          <Pencil className="h-3 w-3" />
        </Button>
      )}
    </div>
  )
}
