export interface Player {
  id: string
  name: string
  avatar: string
  email: string
}

export interface SessionInvite {
  playerId: string
  status: "pending" | "accepted" | "declined"
}

export interface Session {
  id: string
  date: string
  time: string
  description?: string
  invites: SessionInvite[]
  createdBy: string
}

export interface User {
  id: string
  name: string
  role: "admin" | "player"
  avatar: string
  email: string
}
