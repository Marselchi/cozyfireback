"use client"

import type React from "react"
import { memo, useState } from "react"
import { X, Plus, FileText, Eye, Search } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { useBlockLayout } from "./block-layout-context"
import type { Block, BlockType } from "@/types/editor-layout"

interface BlockWrapperProps {
  block: Block
  children: React.ReactNode
}

const typeLabels: Record<string, string> = {
  editor: "Редактор",
  preview: "Превью",
  lore: "Лор",
};

const blockOptions: { type: BlockType; label: string; icon: typeof FileText; description: string }[] = [
  {
    type: "preview",
    label: "Превью",
    icon: Eye,
    description: "Превью лора",
  },
  {
    type: "lore",
    label: "Лор",
    icon: Search,
    description: "Поиск лора",
  },
]

export const BlockWrapper = memo(
  function BlockWrapper({ block, children }: BlockWrapperProps) {
    const { removeBlock, addBlock, canAddBlock, hasPreviewBlock, canAddAtPosition } = useBlockLayout()
    const [menuOpen, setMenuOpen] = useState(false)
    const [selectedPosition, setSelectedPosition] = useState<"top" | "bottom" | "left" | "right" | null>(null)

    const canDelete = block.type !== "editor"
    const showAddButton = canAddBlock()

    const handlePositionSelect = (position: "top" | "bottom" | "left" | "right") => {
      setSelectedPosition(position)
    }

    const handleAddBlock = (type: BlockType) => {
      if (selectedPosition) {
        addBlock(type, selectedPosition, block.id)
      }
      setMenuOpen(false)
      setSelectedPosition(null)
    }

    const handleOpenChange = (open: boolean) => {
      setMenuOpen(open)
      if (!open) {
        setSelectedPosition(null)
      }
    }

    const canAddTop = canAddAtPosition("top")
    const canAddBottom = canAddAtPosition("bottom")
    const canAddLeft = canAddAtPosition("left")
    const canAddRight = canAddAtPosition("right")

    return (
      <div className="group relative flex h-full min-h-50 flex-col rounded-lg border border-border bg-card shadow-sm">
        <div className="flex shrink-0 items-center border-b border-border px-3 py-2">
          <span className="text-sm font-medium text-muted-foreground">
            {typeLabels[block.type]}
          </span>
          <div className="flex items-center gap-1 ml-4">
            {showAddButton && (
              <Popover open={menuOpen} onOpenChange={handleOpenChange}>
                <PopoverTrigger asChild>
                  <Button variant="ghost" size="icon" className="h-6 w-6">
                    <Plus className="h-4 w-4" />
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-48 p-2" align="end">
                  {selectedPosition === null ? (
                    <div className="space-y-1">
                      <p className="mb-2 px-2 text-xs font-medium text-muted-foreground">Куда добавить блок</p>
                      <button
                        onClick={() => handlePositionSelect("top")}
                        disabled={!canAddTop}
                        className="flex w-full items-center gap-2 rounded-md px-2 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        Сверху
                      </button>
                      <button
                        onClick={() => handlePositionSelect("bottom")}
                        disabled={!canAddBottom}
                        className="flex w-full items-center gap-2 rounded-md px-2 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        Снизу
                      </button>
                      <button
                        onClick={() => handlePositionSelect("left")}
                        disabled={!canAddLeft}
                        className="flex w-full items-center gap-2 rounded-md px-2 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        Слева
                      </button>
                      <button
                        onClick={() => handlePositionSelect("right")}
                        disabled={!canAddRight}
                        className="flex w-full items-center gap-2 rounded-md px-2 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        Справа
                      </button>
                    </div>
                  ) : (
                    <div className="space-y-1">
                      <button
                        onClick={() => setSelectedPosition(null)}
                        className="mb-2 flex items-center gap-1 px-2 text-xs font-medium text-muted-foreground hover:text-foreground"
                      >
                        ← Назад
                      </button>
                      <p className="mb-2 px-2 text-xs font-medium text-muted-foreground">Выберите тип блока</p>
                      {blockOptions.map((option) => {
                        const isDisabled = option.type === "preview" && hasPreviewBlock()
                        return (
                          <button
                            key={option.type}
                            onClick={() => handleAddBlock(option.type)}
                            disabled={isDisabled}
                            className="flex w-full items-center gap-3 rounded-md px-2 py-2 text-sm transition-colors hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            <option.icon className="h-4 w-4 text-muted-foreground" />
                            <div className="text-left">
                              <div className="font-medium">{option.label}</div>
                              <div className="text-xs text-muted-foreground">
                                {isDisabled ? "Невозможно два превью" : option.description}
                              </div>
                            </div>
                          </button>
                        )
                      })}
                    </div>
                  )}
                </PopoverContent>
              </Popover>
            )}
            {canDelete && (
              <Button
                variant="ghost"
                size="icon"
                onClick={() => removeBlock(block.id)}
                className="h-6 w-6 text-muted-foreground hover:text-destructive"
              >
                <X className="h-4 w-4" />
              </Button>
            )}
          </div>
        </div>

        <div className="flex-1 overflow-auto overflow-x-hidden">{children}</div>
      </div>
    )
  },
  (prevProps, nextProps) => {
    // Only re-render if block ID changes
    return prevProps.block.id === nextProps.block.id
  },
)
