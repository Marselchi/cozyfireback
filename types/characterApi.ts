import { Excerpt } from './lore'
import { PageableParams, Paginated } from './springTypes'

export interface CharacterRole {
  id: number
  name: string
}

export interface CharacterExcerpt {
  loreId: number
  loreTitle: string
  excerpt: string
  link: string
}

export interface CharacterRequest {
  name: string
  description?: string
  status?: string
  content?: string
  roleIds?: number[]
}

export interface CharacterEditResponse {
  id: number
  name: string
  description: string
  status: string
  content: string
  accountName: string
  createdByRoomCreator: boolean
  roles: CharacterRole[]
}

export interface CharacterInlineResponse {
  id: number
  name: string
  description: string
  status: string
  content: string
  isAuthor: boolean
  accountName: string
  createdByRoomCreator: boolean
  roles: CharacterRole[]
  excerpts: Excerpt[]
  questionCount: number
}

export interface CharacterListResponse {
  id: number
  name: string
  description: string
  status: string
  accountName: string
  createdByRoomCreator: boolean
  roles: CharacterRole[]
}

export interface CharacterListFilter {
  name?: string | null
  roleIds?: number[] | null
  createdByRoomCreator?: boolean | null
}

export interface PaginatedCharactersResponse extends Paginated<CharacterListResponse> {}

export interface CharactersFilterParams extends CharacterListFilter, PageableParams {}
