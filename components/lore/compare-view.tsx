"use client"

import { useState, useRef, useEffect, useMemo, useCallback } from "react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Alert, AlertDescription } from "@/components/ui/alert"
import type { DeletedSpoiler, EditingState, HistoryState, RoleText, User } from "@/types/text"
import { diffChars } from "diff"
import { PreviewSection } from "../edit/preview-section"
import { DeletedSpoilersPanel } from "./deleted-spoilers-panel"
import { DroppableTextarea } from "./droppable-textarea"
import { parseContentWithoutSpoilers } from "@/lib/no-spoiler-parser"
import { AlertTriangle } from "lucide-react"
import type { JSX } from "react/jsx-runtime" // Import JSX to fix the undeclared variable error

type ViewMode = "default" | "preview" | "diff"



interface CompareViewProps {
  editingStates: {
    previous: EditingState
    current: EditingState
  }
  currentUser: User
  availableRoles: RoleText[]
}

export function CompareView({ editingStates, currentUser, availableRoles }: CompareViewProps) {
  const [viewMode, setViewMode] = useState<ViewMode>("default")
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [currentEditingState, setCurrentEditingState] = useState<EditingState>(editingStates.current)
  const [showWarning, setShowWarning] = useState(false)

  const [history, setHistory] = useState<HistoryState[]>([])
  const [historyIndex, setHistoryIndex] = useState(-1)

  const [deletedSpoilers, setDeletedSpoilers] = useState<DeletedSpoiler[]>(() => {
    // Parse current content to extract spoilers (don't remove from previous - it's read-only)
    const currentParsed = parseContentWithoutSpoilers(editingStates.current.content, availableRoles)

    // Convert removed blocks to deleted spoilers format
    const spoilers: DeletedSpoiler[] = []

    // Only add spoilers from current content that need to be managed
    currentParsed.removedBlocks.forEach((block, index) => {
      spoilers.push({
        id: `curr-${index}-${block.id}`,
        content: block.block.content,
        allowedRoles: block.block.allowedRoles,
        isAllocated: false,
      })
    })

    return spoilers
  })

  const [parsedPreviousContent] = useState(() => {
    return editingStates.previous.content
  })

  const [parsedCurrentContent, setParsedCurrentContent] = useState(() => {
    const parsed = parseContentWithoutSpoilers(editingStates.current.content, availableRoles)
    return parsed.content
  })

  const leftScrollRef = useRef<HTMLDivElement>(null)
  const rightScrollRef = useRef<HTMLDivElement>(null)

  const diffContent = useMemo(() => {
    return diffChars(parsedPreviousContent, parsedCurrentContent)
  }, [parsedPreviousContent, parsedCurrentContent])

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === "z" && !e.shiftKey) {
        e.preventDefault()
        handleUndo()
      } else if ((e.ctrlKey || e.metaKey) && (e.key === "y" || (e.key === "z" && e.shiftKey))) {
        e.preventDefault()
        handleRedo()
      }
    }

    document.addEventListener("keydown", handleKeyDown)
    return () => document.removeEventListener("keydown", handleKeyDown)
  }, [])

  const saveToHistory = useCallback(
    (content: string, spoilers: DeletedSpoiler[]) => {
      const newState: HistoryState = { content, spoilers: [...spoilers] }
      setHistory((prev) => {
        const newHistory = prev.slice(0, historyIndex + 1)
        newHistory.push(newState)
        return newHistory.slice(-50) // Keep last 50 states
      })
      setHistoryIndex((prev) => Math.min(prev + 1, 49))
    },
    [historyIndex],
  )

  const handleUndo = useCallback(() => {
    if (historyIndex > 0) {
      const prevState = history[historyIndex - 1]
      setParsedCurrentContent(prevState.content)
      setDeletedSpoilers(prevState.spoilers)
      setHistoryIndex((prev) => prev - 1)

      // Update editing state
      setCurrentEditingState((prev) => ({
        ...prev,
        content: prevState.content,
        hasChanges: prevState.content !== parsedPreviousContent,
      }))
    }
  }, [history, historyIndex, parsedPreviousContent])

  const handleRedo = useCallback(() => {
    if (historyIndex < history.length - 1) {
      const nextState = history[historyIndex + 1]
      setParsedCurrentContent(nextState.content)
      setDeletedSpoilers(nextState.spoilers)
      setHistoryIndex((prev) => prev + 1)

      // Update editing state
      setCurrentEditingState((prev) => ({
        ...prev,
        content: nextState.content,
        hasChanges: nextState.content !== parsedPreviousContent,
      }))
    }
  }, [history, historyIndex, parsedPreviousContent])

  useEffect(() => {
    setCurrentEditingState({
      ...editingStates.current,
      content: parsedCurrentContent,
    })
  }, [editingStates.current, parsedCurrentContent])

  const handleContentChange = useCallback(
    (newContent: string) => {
      setParsedCurrentContent(newContent)
      const updatedState: EditingState = {
        ...currentEditingState,
        content: newContent,
        hasChanges: newContent !== parsedPreviousContent,
      }
      setCurrentEditingState(updatedState)

      saveToHistory(newContent, deletedSpoilers)
    },
    [currentEditingState, parsedPreviousContent, saveToHistory, deletedSpoilers],
  )

  const handleSpoilerDrop = useCallback(
    (spoilerId: string, targetTextArea: "left" | "right", position: number) => {
      const spoiler = deletedSpoilers.find((s) => s.id === spoilerId)
      if (!spoiler) return

      if (spoiler.isAllocated && targetTextArea === "right") {
        // Repositioning an already allocated spoiler
        const rolesStr = spoiler.allowedRoles.map((r) => r.id).join(",")
        const spoilerMarkup = `\n[//]: # (vsbS:[${rolesStr}])\n${spoiler.content}\n[//]: # (vsbE:[${rolesStr}])\n`

        // Remove existing spoiler from content
        let contentWithoutSpoiler = parsedCurrentContent
        if (spoiler.position !== undefined) {
          const beforeSpoiler = parsedCurrentContent.slice(0, spoiler.position)
          const afterSpoiler = parsedCurrentContent.slice(spoiler.position + spoilerMarkup.length)
          contentWithoutSpoiler = beforeSpoiler + afterSpoiler

          // Adjust position if inserting after the removed spoiler
          if (position > spoiler.position) {
            position -= spoilerMarkup.length
          }
        }

        // Insert at new position
        const newContent =
          contentWithoutSpoiler.slice(0, position) + spoilerMarkup + contentWithoutSpoiler.slice(position)

        // Save to history before making changes
        saveToHistory(parsedCurrentContent, deletedSpoilers)

        handleContentChange(newContent)

        // Update spoiler position without changing allocation status
        setDeletedSpoilers((prev) => prev.map((s) => (s.id === spoilerId ? { ...s, position } : s)))
      } else if (!spoiler.isAllocated && targetTextArea === "right") {
        // Allocating a new spoiler
        const rolesStr = spoiler.allowedRoles.map((r) => r.id).join(",")
        const spoilerMarkup = `\n[//]: # (vsbS:[${rolesStr}])\n${spoiler.content}\n[//]: # (vsbE:[${rolesStr}])\n`

        // Insert into current editing content
        const newContent =
          parsedCurrentContent.slice(0, position) + spoilerMarkup + parsedCurrentContent.slice(position)

        // Save to history before making changes
        saveToHistory(parsedCurrentContent, deletedSpoilers)

        handleContentChange(newContent)

        // Mark spoiler as allocated and save position
        setDeletedSpoilers((prev) => prev.map((s) => (s.id === spoilerId ? { ...s, isAllocated: true, position } : s)))
      }

      setShowWarning(false)
    },
    [deletedSpoilers, parsedCurrentContent, handleContentChange, saveToHistory],
  )

  const handleSpoilerAllocate = useCallback((spoilerId: string) => {
    setDeletedSpoilers((prev) => prev.map((s) => (s.id === spoilerId ? { ...s, isAllocated: true } : s)))
  }, [])

  const renderDiffContent = useMemo(() => {
    const lines: JSX.Element[] = []
    let currentLine: JSX.Element[] = []
    let lineIndex = 0

    diffContent.forEach((change, changeIndex) => {
      const chars = change.value.split("")

      chars.forEach((char, charIndex) => {
        const span = (
          <span
            key={`${changeIndex}-${charIndex}`}
            className={
              change.added
                ? "bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-300"
                : change.removed
                  ? "bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-300"
                  : ""
            }
          >
            {char}
          </span>
        )

        if (char === "\n") {
          // End current line and start new one
          lines.push(
            <div key={lineIndex} className="font-mono text-sm whitespace-pre-wrap">
              {currentLine}
            </div>,
          )
          currentLine = []
          lineIndex++
        } else {
          currentLine.push(span)
        }
      })
    })

    // Add remaining characters as final line
    if (currentLine.length > 0) {
      lines.push(
        <div key={lineIndex} className="font-mono text-sm whitespace-pre-wrap">
          {currentLine}
        </div>,
      )
    }

    return lines
  }, [diffContent])

  const handleConfirm = async () => {
    const unallocatedSpoilers = deletedSpoilers.filter((s) => !s.isAllocated)
    if (unallocatedSpoilers.length > 0) {
      setShowWarning(true)
      return
    }

    setIsSubmitting(true)
    try {
      // Server action implementation
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleModeChange = useCallback((mode: ViewMode) => {
    setViewMode(mode)
  }, [])

  const getLeftColumnTitle = () => {
    switch (viewMode) {
      case "diff":
        return "Предложенная правка"
      case "preview":
        return "Оригинальный превью"
      default:
        return "Оригинальный текст"
    }
  }

  const getRightColumnTitle = () => {
    switch (viewMode) {
      case "diff":
        return "Разница"
      case "preview":
        return "Текущий превью"
      default:
        return "Предложенная правка"
    }
  }

  const unallocatedCount = deletedSpoilers.filter((s) => !s.isAllocated).length

  return (
    <div className="flex h-full">
      {/* Main Content Area */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-input">
          <div className="flex items-center gap-2">
            <Button
              variant={viewMode === "default" ? "default" : "outline"}
              size="sm"
              onClick={() => handleModeChange("default")}
            >
              Сырой текст
            </Button>
            <Button
              variant={viewMode === "preview" ? "default" : "outline"}
              size="sm"
              onClick={() => handleModeChange("preview")}
            >
              Превью
            </Button>
            <Button
              variant={viewMode === "diff" ? "default" : "outline"}
              size="sm"
              onClick={() => handleModeChange("diff")}
            >
              Разница
            </Button>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="sm" onClick={handleUndo} disabled={historyIndex <= 0} title="(Ctrl+Z)">
              Откат
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleRedo}
              disabled={historyIndex >= history.length - 1}
              title="(Ctrl+Y)"
            >
              Вернуть
            </Button>
            <Button variant="outline" onClick={() => {}}>
              Отмена
            </Button>
            <Button variant="outline" onClick={() => {}}>
              Отклонить
            </Button>
            <Button onClick={handleConfirm} disabled={isSubmitting}>
              {isSubmitting ? "Подтверждаю..." : "Подтвердить"}
            </Button>
          </div>
        </div>

        {showWarning && unallocatedCount > 0 && (
          <Alert className="m-4 border-amber-200 bg-amber-50 dark:border-amber-800 dark:bg-amber-900/20">
            <AlertTriangle className="h-4 w-4 text-amber-600" />
            <AlertDescription className="text-amber-800 dark:text-amber-200">
              У вас {unallocatedCount} нераспределенных спойлеров.
              Перенесите их все
            </AlertDescription>
          </Alert>
        )}

        <div className="flex-1 flex overflow-hidden">
          {/* Left Column */}
          <div className="flex-1 flex flex-col">
            <div className="p-4 border-r-2 border-input bg-accent flex-shrink-0">
              <h3 className="font-semibold">{getLeftColumnTitle()}</h3>
            </div>
            <div ref={leftScrollRef} className="flex-1 overflow-auto border-r border-input">
              {viewMode === "preview" ? (
                <div className="p-4">
                  <PreviewSection editingState={editingStates.previous} user={currentUser} />
                </div>
              ) : viewMode === "diff" ? (
                <DroppableTextarea
                  value={parsedCurrentContent}
                  onChange={handleContentChange}
                  onSpoilerDrop={(spoilerId, position) => handleSpoilerDrop(spoilerId, "left", position)}
                  className="h-full w-full resize-none font-mono text-sm"
                  style={{
                    minHeight: "100%",
                    lineHeight: "1.5",
                    padding: "16px",
                    outline: "none",
                    boxShadow: "none",
                  }}
                  placeholder="Введите свой текст..."
                />
              ) : (
                <DroppableTextarea
                  value={parsedPreviousContent}
                  onChange={() => {}} // Read only
                  onSpoilerDrop={() => {}} // No drops on read-only
                  readOnly
                  className="h-full w-full resize-none font-mono text-sm bg-muted cursor-not-allowed opacity-75"
                  style={{
                    minHeight: "100%",
                    lineHeight: "1.5",
                    padding: "16px",
                    outline: "none",
                    boxShadow: "none",
                  }}
                />
              )}
            </div>
          </div>

          {/* Right Column */}
          <div className="flex-1 flex flex-col">
            <div className="p-4 border-input bg-accent flex-shrink-0">
              <h3 className="font-semibold">{getRightColumnTitle()}</h3>
            </div>
            <div ref={rightScrollRef} className="flex-1 overflow-auto border-input">
              {viewMode === "preview" ? (
                <div className="p-4">
                  <PreviewSection editingState={currentEditingState} user={currentUser} />
                </div>
              ) : viewMode === "diff" ? (
                <Card className="p-4 h-full w-full">
                  <div className="space-y-1">{renderDiffContent}</div>
                </Card>
              ) : (
                <DroppableTextarea
                  value={parsedCurrentContent}
                  onChange={handleContentChange}
                  onSpoilerDrop={(spoilerId, position) => handleSpoilerDrop(spoilerId, "right", position)}
                  className="h-full w-full resize-none font-mono text-sm"
                  style={{
                    minHeight: "100%",
                    lineHeight: "1.5",
                    padding: "16px",
                    outline: "none",
                    boxShadow: "none",
                  }}
                  placeholder="Введите текст..."
                />
              )}
            </div>
          </div>
          {viewMode === "default" && (
        <div className="flex flex-1 border-l border-input flex-shrink-0">
          <DeletedSpoilersPanel
            deletedSpoilers={deletedSpoilers}
            onSpoilerDrop={handleSpoilerDrop}
            onSpoilerAllocate={handleSpoilerAllocate}
          />
        </div>
      )}
        </div>
      </div>

    </div>
  )
}
