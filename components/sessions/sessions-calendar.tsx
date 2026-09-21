"use client"

import { useState, useMemo } from "react"
import {
  startOfMonth,
  endOfMonth,
  startOfWeek,
  endOfWeek,
} from "date-fns"
import { CalendarGrid } from "./calendar-grid"
import { SessionModalWrapper } from "./session-modal-wrapper"
import { Skeleton } from "@/components/ui/skeleton"
import type { IdName } from "@/types/springTypes"
import { useSessionsByRange } from "@/hooks/use-sessions"

interface SessionsCalendarProps {
  roomName: string
  roomUsers: IdName[]
}

function CalendarSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <Skeleton className="h-8 w-40 bg-muted" />
        <div className="flex gap-2">
          <Skeleton className="h-9 w-9 bg-muted" />
          <Skeleton className="h-9 w-9 bg-muted" />
        </div>
      </div>
      <div className="grid grid-cols-7 gap-1">
        {Array.from({ length: 7 }).map((_, i) => (
          <Skeleton key={i} className="h-6 bg-muted" />
        ))}
      </div>
      <div className="grid grid-cols-7 gap-1">
        {Array.from({ length: 35 }).map((_, i) => (
          <Skeleton key={i} className="h-20 bg-muted rounded-lg" />
        ))}
      </div>
    </div>
  )
}

export function SessionsCalendar({ roomName, roomUsers }: Readonly<SessionsCalendarProps>) {
  const [currentMonth, setCurrentMonth] = useState(new Date())

  const { start, end } = useMemo(() => {
    const monthStart = startOfMonth(currentMonth)
    const monthEnd = endOfMonth(currentMonth)
    const calendarStart = startOfWeek(monthStart, { weekStartsOn: 1 })
    const calendarEnd = endOfWeek(monthEnd, { weekStartsOn: 1 })
    return {
      start: calendarStart.toISOString(),
      end: new Date(calendarEnd.setHours(23, 59, 59, 999)).toISOString(),
    }
  }, [currentMonth])

  const { data: sessions = [], isLoading } = useSessionsByRange(roomName, start, end)

  if (isLoading) return <CalendarSkeleton />

  return (
    <>
      <CalendarGrid
        sessions={sessions}
        roomName={roomName}
        currentMonth={currentMonth}
        onMonthChange={setCurrentMonth}
      />
      <SessionModalWrapper roomName={roomName} roomUsers={roomUsers} />
    </>
  )
}
