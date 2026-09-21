export interface NavRoom {
  id: string
  name: string
  slug: string
  isAdmin: boolean
  description?: string
  questionCount?: number
}

export interface NavUser {
  id: string
  name: string
}

export type Section = "main" | "lore" | "questions" | "characters" | "sessions" | "admin"

export interface RoomSection {
  id: Section
  label: string
  shortLabel?: string
  href: (roomSlug: string) => string
  icon: React.ReactNode
  adminOnly?: boolean
}

export interface Room {
  id: string;
  url: string;
  displayName: string;
}

export interface RoomUserData {
  id: string;
  name: string;
  isAdmin: boolean;
}

export interface NavigationItem {
  href: string;
  label: string;
}


