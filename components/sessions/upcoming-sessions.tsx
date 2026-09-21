"use client"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { useUpcomingSessions } from "@/hooks/use-sessions"
import { UpcomingSessionCard } from "./upcoming-session-card"

interface UpcomingSessionsProps {
  roomName: string
}

function UpcomingSessionsSkeleton() {
  return (
    <Card className="bg-card border-border">
      <CardHeader>
        <Skeleton className="h-6 w-40 bg-muted" />
      </CardHeader>
      <CardContent className="space-y-4">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-24 bg-muted rounded-lg" />
        ))}
      </CardContent>
    </Card>
  )
}

function UpcomingSessionsContent({ roomName }: { roomName: string }) {
  const { data: sessions = [], isLoading } = useUpcomingSessions(roomName)

  if (isLoading) return <UpcomingSessionsSkeleton />

  return (
    <Card className="bg-card border-border">
      <CardHeader className="pb-3">
        <CardTitle className="text-lg font-semibold text-foreground flex items-center gap-2">
          Предстоящие сессии
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {sessions.length === 0 ? (
          <p className="text-sm text-muted-foreground text-center py-4">Нет предстоящих сессий</p>
        ) : (
          sessions.map((session) => (
            <UpcomingSessionCard key={session.id} session={session} />
          ))
        )}
      </CardContent>
    </Card>
  )
}

export function UpcomingSessions({ roomName }: Readonly<UpcomingSessionsProps>) {
  return <UpcomingSessionsContent roomName={roomName} />
  
}
