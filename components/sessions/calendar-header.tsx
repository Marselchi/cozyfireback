"use client"

import { ChevronLeft, ChevronRight } from "lucide-react"
import { Button } from "@/components/ui/button"
import { format } from "date-fns"
import { ru } from "date-fns/locale"

interface CalendarHeaderProps {
  currentMonth: Date
  onPreviousMonth: () => void
  onNextMonth: () => void
}

export function CalendarHeader({ currentMonth, onPreviousMonth, onNextMonth }: Readonly<CalendarHeaderProps>) {
  return (
    <div className="flex items-center justify-between mb-6">
      <h2 className="text-2xl font-semibold tracking-tight text-foreground">{format(currentMonth, "MMMM yyyy", {locale: ru})}</h2>
      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          size="icon"
          onClick={onPreviousMonth}
          className="h-9 w-9 border-border bg-secondary hover:bg-muted"
        >
          <ChevronLeft className="h-4 w-4" />
          <span className="sr-only">Предыдущий месяц</span>
        </Button>
        <Button
          variant="outline"
          size="icon"
          onClick={onNextMonth}
          className="h-9 w-9 border-border bg-secondary hover:bg-muted"
        >
          <ChevronRight className="h-4 w-4" />
          <span className="sr-only">Следующий месяц</span>
        </Button>
      </div>
    </div>
  )
}
