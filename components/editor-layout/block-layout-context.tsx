"use client"

import { createContext, useContext, useState, useCallback, type ReactNode, useMemo, useRef } from "react"
import type { Block, BlockDirection, BlockType, LayoutConfig } from "@/types/editor-layout"

interface BlockLayoutContextType {
  layout: LayoutConfig
  addBlock: (type: BlockType, position: BlockDirection, referenceBlockId: string) => boolean
  removeBlock: (blockId: string) => boolean
  canAddBlock: () => boolean
  hasPreviewBlock: () => boolean
  getGridDimensions: () => { rows: number; cols: number }
  canAddAtPosition: (position: BlockDirection) => boolean
  getBlockById: (id: string) => Block | undefined
}

const BlockLayoutContext = createContext<BlockLayoutContextType | null>(null)

export function useBlockLayout() {
  const context = useContext(BlockLayoutContext)
  if (!context) {
    throw new Error("useBlockLayout must be used within BlockLayoutProvider")
  }
  return context
}
//TODO: substr to slice from, from+to
function generateBlockId(type: BlockType): string {
  return `${type}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
}

const initialLayout: LayoutConfig = {
  blocks: [
    {
      id: "editor-initial",
      type: "editor",
      position: { row: 0, col: 0 },
      size: { rowSpan: 1, colSpan: 1 },
    },
  ],
  maxBlocks: 6,
  maxRows: 3,
  maxCols: 3,
}

export function BlockLayoutProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [layout, setLayout] = useState<LayoutConfig>(initialLayout)

  const blocksMapRef = useRef<Map<string, Block>>(new Map(initialLayout.blocks.map((b) => [b.id, b])))

  const getBlockById = useCallback((id: string) => {
    return blocksMapRef.current.get(id)
  }, [])

  const getGridDimensions = useCallback(() => {
    if (layout.blocks.length === 0) return { rows: 0, cols: 0 }

    const rows = new Set<number>()
    const cols = new Set<number>()

    layout.blocks.forEach((b) => {
      rows.add(b.position.row)
      cols.add(b.position.col)
    })

    return { rows: rows.size, cols: cols.size }
  }, [layout.blocks])

  const hasEditorBlock = useCallback(() => {
    return layout.blocks.some((b) => b.type === "editor")
  }, [layout.blocks])

  const hasPreviewBlock = useCallback(() => {
    return layout.blocks.some((b) => b.type === "preview")
  }, [layout.blocks])

  const canAddBlock = useCallback(() => {
    return layout.blocks.length < layout.maxBlocks
  }, [layout])

  const canAddAtPosition = useCallback(
    (position: BlockDirection) => {
      if (layout.blocks.length >= layout.maxBlocks) return false

      const { rows, cols } = getGridDimensions()

      if (position === "top" || position === "bottom") {
        return rows < layout.maxRows
      }
      if (position === "left" || position === "right") {
        return cols < layout.maxCols
      }
      return false
    },
    [layout.blocks.length, layout.maxBlocks, layout.maxRows, layout.maxCols, getGridDimensions],
  )

  const addBlock = useCallback(
    (type: BlockType, position: BlockDirection, referenceBlockId: string): boolean => {
      if (layout.blocks.length >= layout.maxBlocks) return false
      if (type === "editor" && hasEditorBlock()) return false
      if (type === "preview" && hasPreviewBlock()) return false

      const { rows, cols } = getGridDimensions()

      if ((position === "top" || position === "bottom") && rows >= layout.maxRows) return false
      if ((position === "left" || position === "right") && cols >= layout.maxCols) return false

      const newBlock: Block = {
        id: generateBlockId(type),
        type,
        position: { row: 0, col: 0 },
        size: { rowSpan: 1, colSpan: 1 },
      }

      setLayout((prev) => {
        const refBlock = prev.blocks.find((b) => b.id === referenceBlockId)
        const rows = prev.blocks.map((b) => b.position.row)
        const cols = prev.blocks.map((b) => b.position.col)
        const minRow = Math.min(...rows)
        const maxRow = Math.max(...rows)
        const minCol = Math.min(...cols)
        const maxCol = Math.max(...cols)
        if (!refBlock) return prev

        switch (position) {
          case "top":
            newBlock.position = { row: minRow - 1, col: refBlock.position.col }
            break
          case "bottom":
            newBlock.position = { row: maxRow + 1, col: refBlock.position.col }
            break
          case "left":
            newBlock.position = { row: refBlock.position.row, col: minCol - 1 }
            break
          case "right":
            newBlock.position = { row: refBlock.position.row, col: maxCol + 1 }
            break
        }

        blocksMapRef.current.set(newBlock.id, newBlock)

        return {
          ...prev,
          blocks: [...prev.blocks, newBlock],
        }
      })

      return true
    },
    [layout.blocks.length, layout.maxBlocks, layout.maxRows, layout.maxCols, hasEditorBlock, getGridDimensions],
  )

  const removeBlock = useCallback(
    (blockId: string): boolean => {
      const block = layout.blocks.find((b) => b.id === blockId)
      if (!block || block.type === "editor") return false

      blocksMapRef.current.delete(blockId)

      setLayout((prev) => ({
        ...prev,
        blocks: prev.blocks.filter((b) => b.id !== blockId),
      }))

      return true
    },
    [layout.blocks],
  )

  const contextValue = useMemo(
    () => ({
      layout,
      addBlock,
      removeBlock,
      canAddBlock,
      hasPreviewBlock,
      getGridDimensions,
      canAddAtPosition,
      getBlockById,
    }),
    [layout, addBlock, removeBlock, canAddBlock, hasPreviewBlock, getGridDimensions, canAddAtPosition, getBlockById],
  )

  return <BlockLayoutContext.Provider value={contextValue}>{children}</BlockLayoutContext.Provider>
}
