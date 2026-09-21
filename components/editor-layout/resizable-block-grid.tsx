"use client"

import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "@/components/ui/resizable"
import { useBlockLayout } from "./block-layout-context"
import { BlockWrapper } from "./block-wrapper"
import { PreviewBlock } from "./preview-block"
import type { Block } from "@/types/editor-layout"
import { EditorWrapper } from "../editor/editor-wrapper"
import {  Fragment, memo, useMemo } from "react"
import { EditorContent } from "@/types/editor"
import { LoreBlock } from "./lore-block"

interface ResizableBlockGridProps {
  editorContent?: EditorContent
}

const editorInstancesRef = new Map<string, React.ReactNode>()

const RenderBlock = memo(
  function RenderBlock({
    blockType,
    editorContent,
  }: {
    blockType: string
    editorContent?: EditorContent
  }) {
    if (blockType === "editor") {
      return <EditorWrapper editorContent={editorContent} />
    }

    if (blockType === "lore") {
      return <LoreBlock/>
    }

    if (blockType === "preview"){
      //Maybe add similar to editor ref idk
      return <PreviewBlock/>
    }

    return null
  },
  (prev, next) => prev.blockType === next.blockType,
)

const BlockWithWrapper = memo(
  function BlockWithWrapper({
    block,
    editorContent,
  }: {
    block: Block
    editorContent?: EditorContent
  }) {
    return (
      <BlockWrapper block={block}>
        <RenderBlock blockType={block.type} editorContent={editorContent} />
      </BlockWrapper>
    )
  },
  (prev, next) => prev.block.id === next.block.id && prev.block.type === next.block.type,
)

const RowPanel = memo(
  function RowPanel({
    rowBlocks,
    editorContent,
  }: {
    rowBlocks: Block[]
    editorContent?: EditorContent
  }) {
    return (
      <ResizablePanelGroup orientation="horizontal" className="h-full">
        {rowBlocks.map((block, idx) => (
          <Fragment key={block.id}>
            <ResizablePanel
              id={block.id}
              defaultSize={100 / rowBlocks.length}
              minSize={15}
            >
              <div className="h-full">
                <BlockWithWrapper block={block} editorContent={editorContent} />
              </div>
            </ResizablePanel>

            {idx < rowBlocks.length - 1 && <ResizableHandle withHandle />}
          </Fragment>
        ))}
      </ResizablePanelGroup>
    )
  },
  (prev, next) => {
    if (prev.rowBlocks.length !== next.rowBlocks.length) return false
    // учитывай не только id, но и позицию/тип, иначе можно “пропустить” перестановку
    return prev.rowBlocks.every((b, i) => {
      const nb = next.rowBlocks[i]
      return b.id === nb.id && b.type === nb.type && b.position.col === nb.position.col
    })
  },
)


export function ResizableBlockGrid({ editorContent }: Readonly<ResizableBlockGridProps>) {
  const { layout } = useBlockLayout()

  const { blocksByRow, sortedRows } = useMemo(() => {
    const blocksByRow: Map<number, Block[]> = new Map()

    layout.blocks.forEach((block) => {
      const row = block.position.row
      if (!blocksByRow.has(row)) blocksByRow.set(row, [])
      blocksByRow.get(row)!.push(block)
    })

    blocksByRow.forEach((blocks) => blocks.sort((a, b) => a.position.col - b.position.col))
    const sortedRows = Array.from(blocksByRow.keys()).sort((a, b) => a - b)

    return { blocksByRow, sortedRows }
  }, [layout.blocks])

  if (layout.blocks.length === 0) {
    return (
      <div className="flex h-full items-center justify-center">
        <p className="text-muted-foreground">Нет блоков</p>
      </div>
    )
  }

  return (
    <ResizablePanelGroup orientation="vertical" className="h-full">
      {sortedRows.map((rowIndex, rowIdx) => {
        const rowBlocks = blocksByRow.get(rowIndex)!

        return (
          <Fragment key={`row-${rowIndex}`}>
            <ResizablePanel
              id={`row-${rowIndex}`}
              defaultSize={100 / sortedRows.length}
              minSize={10}
            >
              <RowPanel rowBlocks={rowBlocks} editorContent={editorContent} />
            </ResizablePanel>

            {rowIdx < sortedRows.length - 1 && <ResizableHandle withHandle />}
          </Fragment>
        )
      })}
    </ResizablePanelGroup>
  )
}
