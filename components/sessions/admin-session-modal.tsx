"use client"

import { useState, useTransition, useEffect } from "react"
import { format } from "date-fns"
import { ru } from "date-fns/locale"
import { Clock, FileText, Users, Loader2 } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { useSessionContext } from "./session-context"
import { InviteList } from "./invite-list"
import type { SessionResponse, IdName } from "@/types/springTypes"
import { toast } from "sonner"
import { createSession, updateSession, deleteSession } from "@/server/session/session"
import { useQueryClient } from "@tanstack/react-query"

interface AdminSessionModalProps {
  roomName: string
  existingSession: SessionResponse | null
  roomUsers: IdName[]
}

export function AdminSessionModal({ roomName, existingSession, roomUsers }: Readonly<AdminSessionModalProps>) {
  const { selectedDate, isModalOpen, setIsModalOpen } = useSessionContext()
  const queryClient = useQueryClient()

  const existingTime = existingSession
    ? format(new Date(existingSession.time), "HH:mm")
    : "19:00"

  const [time, setTime] = useState(existingTime)
  const [description, setDescription] = useState(existingSession?.description ?? "")
  const [selectedAccountIds, setSelectedAccountIds] = useState<number[]>(
    existingSession?.participants.map((p) => p.id) ?? [],
  )
  const [isPending, startTransition] = useTransition()

  // Reset form when the modal opens with a (potentially new) session
  useEffect(() => {
    if (!isModalOpen) return
    setTime(existingSession ? format(new Date(existingSession.time), "HH:mm") : "19:00")
    setDescription(existingSession?.description ?? "")
    setSelectedAccountIds(existingSession?.participants.map((p) => p.id) ?? [])
  }, [isModalOpen, existingSession])

  const handleSave = () => {
    if (!selectedDate) return

    const [hours, minutes] = time.split(":").map(Number)
    const dt = new Date(selectedDate)
    dt.setHours(hours, minutes, 0, 0)
    const isoTime = dt.toISOString()

    startTransition(async () => {
      try {
        let updatedSession: SessionResponse | undefined

        if (existingSession) {
          updatedSession = await updateSession(existingSession.id, {
            time: isoTime,
            description,
            accountIds: selectedAccountIds,
          })
          toast("Сессия обновлена", {
            description: `${format(selectedDate, "MMMM d, yyyy", { locale: ru })} в ${time}`,
          })
        } else {
          updatedSession = await createSession(roomName, {
            time: isoTime,
            description,
            accountIds: selectedAccountIds,
          })
          toast("Сессия создана", {
            description: `${format(selectedDate, "MMMM d, yyyy", { locale: ru })} в ${time}`,
          })
        }

        if (updatedSession) {
          // Обновляем кэш upcoming сессий
          queryClient.setQueryData(["upcoming-sessions", roomName], (old: SessionResponse[] = []) => {
            if (existingSession) {
              return old.map(s => s.id === updatedSession.id ? updatedSession : s)
            }
            return [...old, updatedSession]
          })

          // Обновляем кэш сессий по диапазону (для всех месяцев в кэше)
          queryClient.setQueriesData<SessionResponse[]>(
            { queryKey: ["sessions", roomName] },
            (old) => {
              if (!old) return old
              if (existingSession) {
                return old.map(s => s.id === updatedSession.id ? updatedSession: s)
              }
              return [...old, updatedSession]
            }
          )
        }

        setIsModalOpen(false)
      } catch (error) {
        toast("Ошибка", {
          description: "Не удалось сохранить сессию",
        })
      }
    })
  }

  const handleDelete = () => {
    if (!existingSession) return

    startTransition(async () => {
      try {
        await deleteSession(existingSession.id)

        // Удаляем из кэша upcoming сессий
        queryClient.setQueryData(["upcoming-sessions", roomName], (old: SessionResponse[] = []) => {
          return old.filter(s => s.id !== existingSession.id)
        })

        // Удаляем из кэша сессий по диапазону (для всех месяцев в кэше)
        queryClient.setQueriesData<SessionResponse[]>(
          { queryKey: ["sessions", roomName] },
          (old) => {
            if (!old) return old
            return old.filter(s => s.id !== existingSession.id)
          }
        )

        toast("Сессия удалена")
        setIsModalOpen(false)
      } catch (error) {
        toast("Ошибка", {
          description: "Не удалось удалить сессию",
        })
      }
    })
  }

  if (!selectedDate) return null

  return (
    <Dialog open={isModalOpen} onOpenChange={setIsModalOpen}>
      <DialogContent className="sm:max-w-[500px] bg-muted border-border">
        <DialogHeader>
          <DialogTitle className="text-foreground">
            {existingSession ? "Изменить сессию" : "Создать сессию"}
          </DialogTitle>
          <DialogDescription className="text-muted-foreground">
            {format(selectedDate, "EEEE, MMMM d, yyyy", { locale: ru })}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-5 py-4">
          <div className="space-y-2">
            <Label htmlFor="time" className="flex items-center gap-2 text-foreground">
              <Clock className="h-4 w-4 text-muted-foreground" />
              Время сессии
            </Label>
            <Input
              id="time"
              type="time"
              value={time}
              onChange={(e) => setTime(e.target.value)}
              className="bg-secondary border-border focus:border-accent"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="description" className="flex items-center gap-2 text-foreground">
              <FileText className="h-4 w-4 text-muted-foreground" />
              Описание (Опционально)
            </Label>
            <Textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Добавьте описание..."
              className="bg-additional border-border focus:border-accent resize-none"
              rows={3}
            />
          </div>

          <div className="space-y-2">
            <Label className="flex items-center gap-2 text-foreground">
              <Users className="h-4 w-4 text-muted-foreground" />
              Приглашения
            </Label>
            <InviteList
              users={roomUsers}
              selectedIds={selectedAccountIds}
              onSelectionChange={setSelectedAccountIds}
            />
          </div>
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button
            variant="outline"
            onClick={handleDelete}
            disabled={!existingSession}
            className="border-border bg-destructive hover:bg-destructive/90"
          >
            Удалить
          </Button>
          <Button
            variant="outline"
            onClick={() => setIsModalOpen(false)}
            className="border-border bg-additional hover:bg-secondary"
          >
            Отмена
          </Button>
          <Button
            onClick={handleSave}
            disabled={isPending || selectedAccountIds.length === 0}
            className="bg-accent text-accent-foreground hover:bg-secondary"
          >
            {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {existingSession ? "Обновить сессию" : "Создать сессию"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
