import { IdName } from "./springTypes"

export interface LoreItem {
  id: string
  title: string
  description: string
  author: string
  date?: string
  tags: TagText[]
  isFromAdmin: boolean
}

export interface LoreListItem {
  id: number
  title: string
  description: string
  tags: string[]
  author: string
  date: string
  is_created_by_admin: boolean
}

export interface CampaignData {
  currentDate?: string
  currentSituation?: string
  lastSession?: string
}

// edit section
export interface TextData {
  id: number
  title: string
  description: string
  author: string
  date: string
  tags: IdName[]
  nonPublic: boolean
  byAdmin: boolean
  isAuthor: boolean
  viewCount: number
}

export interface ConnectedText {
  id: string
  title: string
}

export interface RoleText {
  id?: string
  name: string
}

export interface TagText {
  id: string
  name: string
}

export interface User {
  id: string
  name: string
  roles: string[]
  isAdmin?: boolean
}

export interface RestrictedBlock {
  id: string
  content: string
  allowedRoles: RoleText[]
  startLine: number
  endLine: number
  startPos: number
  endPos: number
}

export interface ContextMenuPosition {
  x: number
  y: number
  line: number
  cursorPosition: number
}
export interface DeletedSpoiler {
  id: string
  content: string
  allowedRoles: RoleText[]
  isAllocated: boolean
  position?: number
}

export interface HistoryState {
  content: string
  spoilers: DeletedSpoiler[]
}

export interface EditingState {
  id: string
  title: string
  description: string
  author: string
  date: string
  tags: TagText[]
  roles: RoleText[]
  content: string
  restrictedBlocks: RestrictedBlock[]
  isSubmitting: boolean
  hasChanges: boolean
}

export interface ParsedContent {
  content: string
  restrictedBlocks: RestrictedBlock[]
}

