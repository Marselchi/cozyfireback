"use client"

import { useEffect } from "react"
import { useSessionContext } from "./session-context"
import { AdminSessionModal } from "./admin-session-modal"
import { PlayerSessionModal } from "./player-session-modal"
import type { IdName } from "@/types/springTypes"
import { useUser } from "@/lib/contexts/user-context"
import { showToast } from "../utils/toast-utils"

interface SessionModalWrapperProps {
  roomName: string
  roomUsers: IdName[]
}

export function SessionModalWrapper({ roomName, roomUsers }: Readonly<SessionModalWrapperProps>) {
  const { selectedDate, selectedSession, isModalOpen, setIsModalOpen } = useSessionContext()
  const {user} = useUser()

  // Player opens a date with no session
  useEffect(() => {
    if (isModalOpen && !user.isAdmin && !selectedSession) {
      showToast({
        title: "Нет сессии",
        description:"На эту дату сессия не запланирована.",
        type: "info"
      })
      setIsModalOpen(false)
    }
  }, [isModalOpen, user, selectedSession, setIsModalOpen])

  // Player is not invited to the session
  useEffect(() => {
    if (
      isModalOpen &&
      !user.isAdmin &&
      selectedSession &&
      selectedSession.participants.length > 0 &&
      !selectedSession.participants.some((p) => p.id === Number.parseInt(user.id))
    ) {
      showToast({
        title: "Отсутствует приглашение",
        description:"Вас нет в списке приглашенных на эту сессию.",
        type: "info"
      })
      setIsModalOpen(false)
    }
  }, [isModalOpen, user, selectedSession, setIsModalOpen])

  if (!isModalOpen || !selectedDate) return null

  if (user.isAdmin) {
    return (
      <AdminSessionModal
        roomName={roomName}
        existingSession={selectedSession}
        roomUsers={roomUsers}
      />
    )
  }

  if (!selectedSession) return null

  const currentParticipant = selectedSession.participants.find((p) => p.id === Number.parseInt(user.id))
  const isInvited =
    selectedSession.participants.length === 0 || !!currentParticipant
  const currentInviteStatus = currentParticipant
    ? currentParticipant.accepted === true
      ? "accepted"
      : currentParticipant.accepted === false
      ? "declined"
      : "pending"
    : null

  return (
    <PlayerSessionModal
      session={selectedSession}
      roomName={roomName}
      isInvited={isInvited}
      currentInviteStatus={currentInviteStatus as "pending" | "accepted" | "declined" | null}
    />
  )
}
