"use server"

import { mockPlayers, mockSessions } from "./mock-data/mock-data"
import type { Player, Session } from "@/types/sessionPrototype"

// Simulate network delay
const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

export async function getSessions(): Promise<Session[]> {
  await delay(800)
  return mockSessions
}

export async function getPlayers(): Promise<Player[]> {
  await delay(600)
  return mockPlayers
}

export async function getSessionByDate(date: string): Promise<Session | null> {
  await delay(400)
  return mockSessions.find((s) => s.date === date) || null
}

export async function createSession(data: {
  date: string
  time: string
  description?: string
  invitedPlayerIds: string[]
}): Promise<Session> {
  await delay(500)
  const newSession: Session = {
    id: `s${Date.now()}`,
    date: data.date,
    time: data.time,
    description: data.description,
    invites: data.invitedPlayerIds.map((id) => ({ playerId: id, status: "pending" })),
    createdBy: "admin1",
  }
  mockSessions.push(newSession)
  return newSession
}

export async function updateSessionResponse(
  sessionId: string,
  playerId: string,
  response: "accepted" | "declined",
): Promise<boolean> {
  await delay(300)
  const session = mockSessions.find((s) => s.id === sessionId)
  if (session) {
    const invite = session.invites.find((i) => i.playerId === playerId)
    if (invite) {
      invite.status = response
      return true
    }
  }
  return false
}
