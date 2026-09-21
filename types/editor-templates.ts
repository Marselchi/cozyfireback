import { Paginated } from './springTypes'

// ---------------------------------------------------------------------------
// Editor Template Types
// ---------------------------------------------------------------------------

export type EditorTemplate = {
  id: number
  name: string
  createdAt: number
}

export type EditorTemplateList = Paginated<EditorTemplate>
