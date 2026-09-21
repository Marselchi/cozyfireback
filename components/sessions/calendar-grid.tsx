"use client"

import { useMemo } from "react"
import {
  startOfMonth,
  endOfMonth,
  startOfWeek,
  endOfWeek,
  eachDayOfInterval,
  addMonths,
  subMonths,
  format,
} from "date-fns"
import { CalendarHeader } from "./calendar-header"
import { CalendarCell } from "./calendar-cell"
import { useSessionContext } from "./session-context"
import type { SessionResponse } from "@/types/springTypes"

const WEEKDAYS = ["Пон", "Втр", "Сре", "Чет", "Пят", "Суб", "Вос"]

interface CalendarGridProps {
  sessions: SessionResponse[]
  roomName: string
  currentMonth: Date
  onMonthChange: (month: Date) => void
}

export function CalendarGrid({
  sessions,
  roomName,
  currentMonth,
  onMonthChange,
}: Readonly<CalendarGridProps>) {
  const { setSelectedDate, setSelectedSession, setIsModalOpen } = useSessionContext()

  const days = useMemo(() => {
    const monthStart = startOfMonth(currentMonth)
    const monthEnd = endOfMonth(currentMonth)
    const calendarStart = startOfWeek(monthStart, { weekStartsOn: 1 })
    const calendarEnd = endOfWeek(monthEnd, { weekStartsOn: 1 })
    return eachDayOfInterval({ start: calendarStart, end: calendarEnd })
  }, [currentMonth])

  // Sessions are keyed by date portion of their ISO time string
  const sessionMap = useMemo(() => {
    const map = new Map<string, SessionResponse>()
    sessions.forEach((session) => {
      const dateKey = format(new Date(session.time), "yyyy-MM-dd")
      map.set(dateKey, session)
    })
    return map
  }, [sessions])

  const handleDateClick = (date: Date, session: SessionResponse | null) => {
    setSelectedDate(date)
    setSelectedSession(session)
    setIsModalOpen(true)
  }

  return (
    <div className="w-full">
      <CalendarHeader
        currentMonth={currentMonth}
        onPreviousMonth={() => onMonthChange(subMonths(currentMonth, 1))}
        onNextMonth={() => onMonthChange(addMonths(currentMonth, 1))}
      />

      <div className="grid grid-cols-7 gap-0.5 sm:gap-1 mb-1 sm:mb-2">
        {WEEKDAYS.map((day) => (
          <div
            key={day}
            className="text-center text-[10px] sm:text-xs font-medium text-muted-foreground py-1 sm:py-2"
          >
            {day}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-0.5 sm:gap-1">
        {days.map((day) => {
          const dateKey = format(day, "yyyy-MM-dd")
          const session = sessionMap.get(dateKey) ?? null

          return (
            <CalendarCell
              key={dateKey}
              date={day}
              currentMonth={currentMonth}
              session={session}
              onClick={() => handleDateClick(day, session)}
            />
          )
        })}
      </div>
    </div>
  )
}
