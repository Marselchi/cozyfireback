"use client"

import type React from "react"
import { useState, useCallback } from "react"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { ScrollArea } from "@/components/ui/scroll-area"
import { AlertTriangle, GripVertical } from "lucide-react"
import { DeletedSpoiler } from "@/types/text"


interface DeletedSpoilersPanelProps {
  deletedSpoilers: DeletedSpoiler[]
  onSpoilerDrop: (spoilerId: string, targetTextArea: "left" | "right", position: number) => void
  onSpoilerAllocate: (spoilerId: string) => void
  className?: string
}

export function DeletedSpoilersPanel({
  deletedSpoilers,
  onSpoilerDrop,
  onSpoilerAllocate,
  className = "",
}: DeletedSpoilersPanelProps) {
  const [draggedSpoiler, setDraggedSpoiler] = useState<string | null>(null)

  const unallocatedSpoilers = deletedSpoilers.filter((spoiler) => !spoiler.isAllocated)
  const allocatedSpoilers = deletedSpoilers.filter((spoiler) => spoiler.isAllocated)

  const handleDragStart = useCallback((e: React.DragEvent, spoilerId: string) => {

    // Set drag data immediately and synchronously
    e.dataTransfer.effectAllowed = "move"
    e.dataTransfer.setData("text/plain", spoilerId)
    e.dataTransfer.setData("application/x-spoiler-id", spoilerId)

    // Set visual feedback without state
    const element = e.currentTarget as HTMLElement
    element.style.opacity = "0.5"
    element.style.transform = "scale(0.95)"

  }, [])

  const handleDragEnd = useCallback((e: React.DragEvent) => {

    // Reset visual feedback
    const element = e.currentTarget as HTMLElement
    element.style.opacity = "1"
    element.style.transform = "scale(1)"

    setDraggedSpoiler(null)
  }, [])

  const SpoilerCard = ({ spoiler }: { spoiler: DeletedSpoiler }) => {
    return (
      <Card
        className={`p-3 cursor-move transition-all duration-200 select-none hover:shadow-md hover:scale-[1.02] ${
          spoiler.isAllocated
            ? "bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800"
            : "bg-card hover:bg-accent/50"
        }`}
        draggable={true}
        onDragStart={(e) => handleDragStart(e, spoiler.id)}
        onDragEnd={handleDragEnd}
      >
        <div className="flex items-start gap-2">
          <GripVertical className="h-4 w-4 text-muted-foreground mt-1 flex-shrink-0" />
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-2">
              <div className="flex flex-wrap gap-1">
                {spoiler.allowedRoles.map((role) => (
                  <Badge key={role.id} variant="secondary" className="text-xs">
                    {role.name || role.id}
                  </Badge>
                ))}
              </div>
              {spoiler.isAllocated && (
                <Badge variant="default" className="text-xs bg-green-600">
                  Распределено
                </Badge>
              )}
            </div>
            <p className="text-sm text-muted-foreground line-clamp-3 font-mono">
              {spoiler.content.trim() || "(Пустой спойлер)"}
            </p>
          </div>
        </div>
      </Card>
    )
  }

  return (
    <div className={`flex flex-col h-full ${className}`}>
      <div className="p-4 border-b border-input bg-accent">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold">Спойлеры</h3>
          {unallocatedSpoilers.length > 0 && (
            <div className="flex items-center gap-2 text-amber-600 dark:text-amber-400">
              <AlertTriangle className="h-4 w-4" />
              <span className="text-sm font-medium">{unallocatedSpoilers.length} Нераспределенных</span>
            </div>
          )}
        </div>
      </div>

      <ScrollArea className="flex-1">
        <div className="p-4 space-y-3">
          {unallocatedSpoilers.length > 0 && (
            <div>
              <h4 className="text-sm font-medium text-muted-foreground mb-2">
                Нераспределенных ({unallocatedSpoilers.length})
              </h4>
              <div className="space-y-2">
                {unallocatedSpoilers.map((spoiler) => (
                  <SpoilerCard key={spoiler.id} spoiler={spoiler} />
                ))}
              </div>
            </div>
          )}

          {allocatedSpoilers.length > 0 && (
            <div>
              <h4 className="text-sm font-medium text-muted-foreground mb-2">Распределено ({allocatedSpoilers.length})</h4>
              <div className="space-y-2">
                {allocatedSpoilers.map((spoiler) => (
                  <SpoilerCard key={spoiler.id} spoiler={spoiler} />
                ))}
              </div>
            </div>
          )}

          {deletedSpoilers.length === 0 && (
            <div className="text-center py-8 text-muted-foreground">
              <p className="text-sm">Спойлеров не было</p>
            </div>
          )}
        </div>
      </ScrollArea>
    </div>
  )
}
