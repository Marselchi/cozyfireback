import { IdName } from "./springTypes"

export interface Tag {
  id: string
  name: string
}

export interface Role {
  id: string
  name: string
}

export type ItemType = "tag" | "role"

export interface RoomAccountResponse {
  id: number
  username: string
  profileName: string
  roles: IdName[]
}