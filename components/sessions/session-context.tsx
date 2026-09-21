"use client"

import { createContext, useContext, useState, useMemo, type ReactNode } from "react"
import type { SessionResponse } from "@/types/springTypes"

interface SessionContextType {
  selectedDate: Date | null
  setSelectedDate: (date: Date | null) => void
  selectedSession: SessionResponse | null
  setSelectedSession: (session: SessionResponse | null) => void
  isModalOpen: boolean
  setIsModalOpen: (open: boolean) => void
  refreshKey: number
  triggerRefresh: () => void
}

const SessionContext = createContext<SessionContextType | null>(null)

export function SessionProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [selectedDate, setSelectedDate] = useState<Date | null>(null)
  const [selectedSession, setSelectedSession] = useState<SessionResponse | null>(null)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)

  const triggerRefresh = () => setRefreshKey((prev) => prev + 1)

  const value = useMemo(
    () => ({
      selectedDate,
      setSelectedDate,
      selectedSession,
      setSelectedSession,
      isModalOpen,
      setIsModalOpen,
      refreshKey,
      triggerRefresh,
    }),
    [selectedDate, selectedSession, isModalOpen, refreshKey],
  )

  return (
    <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
  )
}

export function useSessionContext() {
  const context = useContext(SessionContext)
  if (!context) {
    throw new Error("useSessionContext must be used within a SessionProvider")
  }
  return context
}
