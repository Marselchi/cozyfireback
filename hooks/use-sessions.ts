"use client"

import { useQuery, useQueryClient } from "@tanstack/react-query"
import { getSessionsByRange, getUpcomingSessions } from "@/server/session/session"
import type { SessionListResponse, SessionResponse } from "@/types/springTypes"

/**
 * Хук для получения сессий за период с кэшированием по месяцам
 */
export function useSessionsByRange(roomName: string, start: string, end: string) {
  return useQuery<SessionResponse[]>({
    queryKey: ["sessions", roomName, start, end],
    queryFn: () => getSessionsByRange(roomName, start, end),
    staleTime: 5 * 60 * 1000, // 5 минут
    retry: 1,
  })
}

/**
 * Хук для получения предстоящих сессий
 */
export function useUpcomingSessions(roomName: string) {
  return useQuery<SessionResponse[]>({
    queryKey: ["upcoming-sessions", roomName],
    queryFn: () => getUpcomingSessions(roomName),
    staleTime: 2 * 60 * 1000, // 2 минуты
    retry: 1,
  })
}
