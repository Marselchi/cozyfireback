"use server";

import { getWithAuth, sendWithAuth } from "@/lib/auth/apiClient";
import type { SessionResponse, SessionRequest } from "@/types/springTypes";

/**
 * Получить сессии за период
 */
export async function getSessionsByRange(
  roomName: string,
  start: string,
  end: string,
): Promise<SessionResponse[]> {
  const [error, data] = await getWithAuth<SessionResponse[]>(
    `/sessions/${roomName}/range?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`,
  );

  if (error) {
    throw new Error(`Failed to get sessions: ${error?.message}`);
  }

  return data ?? [];
}

/**
 * Получить предстоящие сессии (где пользователь участвует или админ)
 */
export async function getUpcomingSessions(
  roomName: string,
): Promise<SessionResponse[]> {
  const [error, data] = await getWithAuth<SessionResponse[]>(
    `/sessions/${roomName}/upcoming`,
  );

  if (error) {
    throw new Error(`Failed to get upcoming sessions: ${error?.message}`);
  }

  return data ?? [];
}

/**
 * Получить полную информацию о сессии по ID
 */
export async function getSessionById(
  sessionId: number,
): Promise<SessionResponse> {
  const [error, data] = await getWithAuth<SessionResponse>(
    `/sessions/${sessionId}`,
  );

  if (error || !data) {
    throw new Error(`Failed to get session: ${error?.message}`);
  }

  return data;
}

/**
 * Создать сессию
 */
export async function createSession(
  roomName: string,
  request: SessionRequest,
): Promise<SessionResponse> {
  const [error, data] = await sendWithAuth<SessionResponse>(
    `/sessions/${roomName}`,
    "POST",
    request,
  );

  if (error) {
    throw new Error(`Failed to create session: ${error?.message}`);
  }

  return data!;
}

/**
 * Обновить сессию
 */
export async function updateSession(
  sessionId: number,
  request: SessionRequest,
): Promise<SessionResponse> {
  const [error, data] = await sendWithAuth<SessionResponse>(
    `/sessions/${sessionId}`,
    "PUT",
    request,
  );

  if (error) {
    throw new Error(`Failed to update session: ${error?.message}`);
  }

  return data!;
}

/**
 * Удалить сессию
 */
export async function deleteSession(sessionId: number): Promise<void> {
  const [error] = await sendWithAuth(`/sessions/${sessionId}`, "DELETE", null, {
    expectsJson: false,
  });

  if (error) {
    throw new Error(`Failed to delete session: ${error?.message}`);
  }
}

/**
 * Принять/отклонить приглашение
 */
export async function acknowledgeSession(
  roomName: string,
  sessionId: number,
  accepted: boolean,
): Promise<void> {
  const [error] = await sendWithAuth(
    `/sessions/${roomName}/${sessionId}/acknowledge?accepted=${accepted}`,
    "POST",
  );

  if (error) {
    throw new Error(`Failed to acknowledge session: ${error?.message}`);
  }
}
