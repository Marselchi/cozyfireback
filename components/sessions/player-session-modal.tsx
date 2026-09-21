"use client"

import { useTransition } from "react"
import { format } from "date-fns"
import { ru } from "date-fns/locale"
import { Calendar, Clock, FileText, Check, X, Loader2, Users } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { useSessionContext } from "./session-context"
import type { SessionResponse } from "@/types/springTypes"
import { cn } from "@/lib/utils"
import { useUser } from "@/lib/contexts/user-context"
import { acknowledgeSession } from "@/server/session/session"
import { useQueryClient } from "@tanstack/react-query"
import { showToast } from "../utils/toast-utils"

interface PlayerSessionModalProps {
  session: SessionResponse
  roomName: string
  isInvited: boolean
  currentInviteStatus: "pending" | "accepted" | "declined" | null
}

export function PlayerSessionModal({
  session,
  roomName,
  isInvited,
  currentInviteStatus,
}: Readonly<PlayerSessionModalProps>) {
  const { selectedDate, isModalOpen, setIsModalOpen } = useSessionContext()
  const {user} = useUser()
  const queryClient = useQueryClient()
  const [isPending, startTransition] = useTransition()

  const handleResponse = (accepted: boolean) => {
    startTransition(async () => {
      // Оптимистичное обновление кэша
      const optimisticUpdate = (old: SessionResponse[] = []) => 
        old.map(s => {
          if (s.id === session.id) {
            return {
              ...s,
              participants: s.participants.map(p => 
                p.id === Number.parseInt(user.id) 
                  ? { ...p, accepted } 
                  : p
              )
            }
          }
          return s
        })
      
      // Обновляем кэш моментально
      queryClient.setQueryData(["upcoming-sessions", roomName], optimisticUpdate)
      queryClient.setQueriesData<SessionResponse[]>(
        { queryKey: ["sessions", roomName] },
        optimisticUpdate
      )

      try {
        await acknowledgeSession(roomName, session.id, accepted)

        showToast({
          title: accepted ? "Участие подтверждено" : "Отказ зафиксирован",
          description:accepted
            ? "Вы подтвердили своё участие в сессии"
            : "Вы отказались от участия в сессии",
          type: "success"
        })
      } catch (error) {
        showToast({
          title: "Ошибка",
          description:"Не удалось обновить статус",
          type: "error"
        })
        queryClient.invalidateQueries({ queryKey: ["upcoming-sessions", roomName] })
        queryClient.invalidateQueries({ queryKey: ["sessions", roomName] })
        console.log(error)
      }

      setIsModalOpen(false)
    })
  }

  if (!selectedDate) return null

  const sessionDate = new Date(session.time)

  const getStatusBadge = () => {
    switch (currentInviteStatus) {
      case "accepted":
        return <Badge className="bg-green-500 text-white">Принял</Badge>
      case "declined":
        return <Badge variant="destructive">Отклонил</Badge>
      case "pending":
        return <Badge variant="secondary">Ожидаем решения</Badge>
      default:
        return null
    }
  }

  const getStatusIcon = (accepted: boolean | null) => {
    if (accepted === true) return <Check className="h-4 w-4 text-green-500" />
    if (accepted === false) return <X className="h-4 w-4 text-red-500" />
    return <Clock className="h-4 w-4 text-orange-500" />
  }

  return (
    <Dialog open={isModalOpen} onOpenChange={setIsModalOpen}>
      <DialogContent className="sm:max-w-125 bg-muted border-border max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-foreground">
            <Calendar className="h-5 w-5 text-accent" />
            Детали сессии
          </DialogTitle>
          <DialogDescription className="text-muted-foreground">
            {format(sessionDate, "EEEE, MMMM d, yyyy", { locale: ru })}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="flex items-center justify-between p-4 rounded-lg bg-additional">
            <div className="flex items-center gap-3">
              <Clock className="h-5 w-5 text-foreground" />
              <div>
                <p className="text-sm text-muted-foreground">Время сессии</p>
                <p className="text-lg font-semibold text-foreground">
                  {format(sessionDate, "HH:mm")}
                </p>
              </div>
            </div>
            {getStatusBadge()}
          </div>

          {session.description && (
            <div className="p-4 rounded-lg bg-additional">
              <div className="flex items-start gap-3">
                <FileText className="h-5 w-5 text-foreground mt-0.5" />
                <div>
                  <p className="text-sm text-muted-foreground mb-1">Описание</p>
                  <p className="text-foreground">{session.description}</p>
                </div>
              </div>
            </div>
          )}


          {session.participants.length > 0 && (
            <div className="p-4 rounded-lg bg-additional">
              <div className="flex items-center gap-2 mb-3">
                <Users className="h-5 w-5 text-muted-foreground" />
                <p className="text-sm font-medium text-foreground">Приглашённые игроки</p>
              </div>
              <div className="space-y-2">
                {session.participants.map(({ id, name, accepted }) => (
                  <div
                    key={id}
                    className="flex items-center justify-between gap-3 p-2 rounded-md bg-background"
                  >
                    <div className="flex items-center gap-3">
                      <Avatar className="h-8 w-8">
                        <AvatarFallback>{name.charAt(0)}</AvatarFallback>
                      </Avatar>
                      <span className="text-sm text-foreground">{name} {id === Number.parseInt(user.id) ? "(вы)" : ""}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      {getStatusIcon(accepted)}
                      <span className="text-xs text-muted-foreground">
                        {accepted === true ? "принял" : accepted === false ? "отклонил" : "ожидание"}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          
          {isInvited && (
            <div className="p-4 rounded-lg border border-accent/30 bg-additional/70">
              <p className="text-sm font-medium">Вы были приглашены на эту сессию</p>
              <p className="text-xs text-muted-foreground mt-1">
                Пожалуйста, выберите сможете ли вы присутствовать
              </p>
            </div>
          )}
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          {isInvited ? (
            <>
              <Button
                variant="outline"
                onClick={() => handleResponse(false)}
                disabled={isPending}
                className={cn(
                  "border-border",
                  currentInviteStatus === "declined" && "bg-destructive/10 border-destructive",
                )}
              >
                {isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <X className="mr-2 h-4 w-4" />}
                Отклонить
              </Button>
              <Button
                onClick={() => handleResponse(true)}
                disabled={isPending}
                className={cn(
                  "bg-accent text-accent-foreground hover:bg-accent/90",
                  currentInviteStatus === "accepted" && "ring-2 ring-accent ring-offset-2 ring-offset-background",
                )}
              >
                {isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Check className="mr-2 h-4 w-4" />}
                Подтвердить
              </Button>
            </>
          ) : (
            <Button
              onClick={() => setIsModalOpen(false)}
              className="bg-secondary text-secondary-foreground hover:bg-muted"
            >
              Закрыть
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
