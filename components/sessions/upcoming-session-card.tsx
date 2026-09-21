"use client"

import { format, parseISO } from "date-fns"
import { Clock, Users, Check, X } from "lucide-react"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { ru } from "date-fns/locale"
import type { SessionResponse } from "@/types/springTypes"
import { useSessionContext } from "./session-context"
import { useUser } from "@/lib/contexts/user-context"

interface UpcomingSessionCardProps {
  session: SessionResponse
}

export function UpcomingSessionCard({ session }: Readonly<UpcomingSessionCardProps>) {
  const { setSelectedDate, setSelectedSession, setIsModalOpen } = useSessionContext()
  const {user} = useUser()

  const handleSessionClick = () => {
    setSelectedDate(parseISO(session.time))
    setSelectedSession(session)
    setIsModalOpen(true)
  }

  const sessionDate = new Date(session.time)
  const currentParticipant = session.participants.find(p => p.id === Number.parseInt(user.id))
  
  const getStatusIcon = () => {
    if (currentParticipant?.accepted === true) {
      return <Check className="h-3 w-3 text-green-500" />
    }
    if (currentParticipant?.accepted === false) {
      return <X className="h-3 w-3 text-red-500" />
    }
    return <Clock className="h-3 w-3 text-orange-500" />
  }

  const acceptedCount = session.participants.filter((p) => p.accepted === true).length
  const totalCount = session.participants.length

  return (
    <button
      onClick={handleSessionClick}
      className="w-full p-4 rounded-lg bg-additional border border-border hover:border-muted-foreground transition-colors text-left"
    >
      <div className="flex items-start justify-between mb-2">
        <div>
          <p className="text-sm font-medium text-foreground">{format(sessionDate, "EEEE, MMM d", { locale: ru })}</p>
          <div className="flex items-center gap-1 text-xs text-muted-foreground mt-1">
            <Clock className="h-3 w-3" />
            {format(sessionDate, "HH:mm")}
          </div>
        </div>
        <div className="flex items-center gap-1">
          <Badge variant="secondary" className="text-xs">
            <Users className="h-3 w-3 mr-1" />
            {acceptedCount}/{totalCount}
          </Badge>
          {!user.isAdmin && (
            <div className="shrink-0">{getStatusIcon()}</div>
          )}
        </div>
      </div>
      {session.description && (
        <p className="text-xs text-muted-foreground mb-3 line-clamp-2">{session.description}</p>
      )}
      {session.participants.length > 0 && (
        <div className="flex -space-x-2">
          {session.participants.slice(0, 4).map((participant) => (
            <Avatar key={participant.id} className="h-6 w-6 border-2 border-card">
              <AvatarFallback className="text-[10px] bg-muted font-medium">
                {participant.name.charAt(0).toUpperCase()}
              </AvatarFallback>
            </Avatar>
          ))}
          {session.participants.length > 4 && (
            <div className="h-6 w-6 rounded-full bg-muted border-2 border-card flex items-center justify-center">
              <span className="text-[10px] text-muted-foreground">+{session.participants.length - 4}</span>
            </div>
          )}
        </div>
      )}
    </button>
  )
}
