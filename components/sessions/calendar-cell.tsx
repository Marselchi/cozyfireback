"use client"

import { cn } from "@/lib/utils"
import { format, isSameMonth, isToday } from "date-fns"
import { Check, X, Clock, Users } from "lucide-react"
import type { SessionResponse } from "@/types/springTypes"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { useUser } from "@/lib/contexts/user-context"
import { Badge } from "../ui/badge"

interface CalendarCellProps {
  date: Date
  currentMonth: Date
  session: SessionResponse | null
  onClick: () => void
}

export function CalendarCell({ date, currentMonth, session, onClick }: Readonly<CalendarCellProps>) {
  const isCurrentMonth = isSameMonth(date, currentMonth)
  const isCurrentDay = isToday(date)
  const hasSession = !!session
  const {user} = useUser()

  const getSessionStatus = () => {
    if (!session) return null

    const currentParticipant = session.participants.find(p => p.id === Number.parseInt(user.id))
    const isInvited = session.participants.length === 0 || !!currentParticipant

    const acceptedCount = session.participants.filter((p) => p.accepted === true).length
    const totalCount = session.participants.length
    const allAccepted = totalCount > 0 && acceptedCount === totalCount
    const someAccepted = acceptedCount > 0 && acceptedCount < totalCount
    const noneAccepted = totalCount === 0 || acceptedCount === 0

    let cellColor = "bg-muted"
    
    if (hasSession) {
      if (noneAccepted) {
        cellColor = "bg-red-500/10 border-red-500/50"
      } else if (someAccepted) {
        cellColor = "bg-yellow-500/10 border-yellow-500/50"
      } else if (allAccepted) {
        cellColor = "bg-green-500/10 border-green-500/50"
      }
    }

    let playerIcon = null
    if (!user.isAdmin && isInvited) {
      if (currentParticipant?.accepted === true) {
        playerIcon = <Check className="h-3 w-3 sm:h-4 sm:w-4 text-green-500" />
      } else if (currentParticipant?.accepted === false) {
        playerIcon = <X className="h-3 w-3 sm:h-4 sm:w-4 text-red-500" />
      } else if (currentParticipant?.accepted === null) {
        playerIcon = <Clock className="h-3 w-3 sm:h-4 sm:w-4 text-orange-500" />
      }
    }

    return { cellColor, playerIcon, acceptedCount, totalCount }
  }

  const sessionStatus = getSessionStatus()

  return (
    <button
      onClick={onClick}
      className={cn(
        "relative aspect-square min-h-10 sm:min-h-0 sm:h-auto p-1 sm:p-2 border border-border rounded-lg transition-all duration-200 bg-muted",
        "hover:border-muted-foreground hover:bg-muted/50 focus:outline-none focus:ring-2 focus:ring-ring",
        "flex flex-col items-start text-left w-full",
        !isCurrentMonth && "opacity-40",
        isCurrentDay && "ring-2 ring-blue-500/60 bg-current/10",
        hasSession && sessionStatus?.cellColor,
      )}
    >
      <span
        className={cn(
          "text-[10px] sm:text-sm font-medium leading-none",
          !isCurrentMonth && "text-muted-foreground",
        )}
      >
        {format(date, "d")}
      </span>

      {hasSession && (
        <div className="mt-0.5 sm:mt-1 w-full flex flex-col flex-1 justify-between min-w-0">
          {/* Time row */}
          <div className="flex items-center gap-0.5">
            <div className="flex items-center gap-0.5 min-w-0">
              <div className="h-1.5 w-1.5 sm:h-2 sm:w-2 rounded-full bg-green-500 animate-pulse shrink-0" />
              <span className="text-[9px] sm:text-xs font-medium truncate">
                {format(new Date(session.time), "HH:mm")}
              </span>
            </div>
          </div>
          {session.description && (
            <p className="hidden sm:block text-[9px] sm:text-xs text-muted-foreground truncate mt-0.5">
              {session.description.slice(0, 20)}…
            </p>
          )}

          {/* Player badges */}
          {session.participants.length > 0 && (
            <div className="flex items-center gap-1 mt-0.5 sm:mt-1">
              <div className="flex -space-x-1">
                {session.participants.slice(0, 4).map((participant) => (
                  <Avatar key={participant.id} className="h-4 w-4 sm:h-5 sm:w-5 border border-card">
                    <AvatarFallback className="text-[8px] sm:text-[10px] bg-muted font-medium">
                      {participant.name.charAt(0).toUpperCase()}
                    </AvatarFallback>
                  </Avatar>
                ))}
                {session.participants.length > 4 && (
                  <div className="h-4 w-4 sm:h-5 sm:w-5 rounded-full bg-muted border border-card flex items-center justify-center">
                    <span className="text-[7px] sm:text-[9px] text-muted-foreground">+{session.participants.length - 4}</span>
                  </div>
                )}
              </div>
              {sessionStatus && (
                <div className="flex items-center gap-0.5 shrink-0 ml-auto">
                  <Badge variant="secondary" className="text-xs">
                    <Users className="h-2.5 w-2.5 sm:h-3 sm:w-3 text-muted-foreground" />
                    {sessionStatus.acceptedCount}/{sessionStatus.totalCount}
                  </Badge>
                </div>
              )}
            </div>
          )}


        </div>
      )}
    </button>
  )
}
