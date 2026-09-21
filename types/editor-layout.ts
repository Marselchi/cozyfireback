// Block types
export type BlockType = "editor" | "preview" | "lore"

export type BlockDirection = "top" | "bottom" | "left" | "right"

export interface Block {
  id: string
  type: BlockType
  position: {
    row: number
    col: number
  }
  size: {
    rowSpan: number
    colSpan: number
  }
}

export interface LayoutConfig {
  blocks: Block[]
  maxBlocks: number
  maxRows: number
  maxCols: number
}

// Lore types for preview block
export interface Lore {
  id: string
  title: string
  summary: string
  content: string
  author: string
  publishedAt: string
  tags: string[]
}

// Metadata types
export interface Tag {
  id: number
  name: string
}

export interface Role {
  id: number
  name: string
  users?: string[]
}

export interface DocumentMetadata {
  name: string
  description: string
  date: string
  selectedTagsId: number[]
  selectedRolesId: number[]
}
