"use server";

import { getWithAuth, sendWithAuth } from "@/lib/auth/apiClient";
export type NotifType = "content" | "qa" | "session";

export interface Notification {
  id: string;
  type: NotifType;
  subtype: string;
  title: string;
  body: string;
  route?: string;
  time: string;
  read: boolean;
}

export type SettingKey =
  | "contentAll"
  | "contentDMOnly"
  | "contentUpdated"
  | "qaNewQuestion"
  | "qaAnswered"
  | "qaAnswerToAnswer"
  | "sessionCreated"
  | "sessionUpdated"
  | "sessionSuccess";

export type DeliveryChannel = "polling" | "push";

export interface NotificationSetting {
  key: SettingKey;
  polling: boolean;
  push: boolean;
}

/* ─── Backend types & maps ────────────────────────────────────────────── */
interface BackendNotification {
  id: number;
  type: string;
  eventCode: string;
  title: string;
  body: string;
  route: string | null;
  readAt: string | null;
  createdAt: string;
}

interface BackendPreference {
  eventCode: string;
  channel: "POLLING" | "PUSH";
  enabled: boolean;
}

const TYPE_MAP: Record<string, NotifType> = {
  CONTENT: "content",
  QA: "qa",
  SESSION: "session",
};
const EVENT_TO_KEY: Record<string, SettingKey> = {
  CONTENT_CREATED: "contentAll",
  CONTENT_UPDATED: "contentUpdated",
  CONTENT_CREATED_DM: "contentDMOnly",
  QA_NEW_QUESTION: "qaNewQuestion",
  QA_ANSWERED: "qaAnswered",
  QA_ANSWER_TO_ANSWER: "qaAnswerToAnswer",
  SESSION_CREATED: "sessionCreated",
  SESSION_UPDATED: "sessionUpdated",
  SESSION_SUCCESS: "sessionSuccess",
};
const KEY_TO_EVENT: Record<SettingKey, string> = Object.fromEntries(
  Object.entries(EVENT_TO_KEY).map(([k, v]) => [v, k]),
) as Record<SettingKey, string>;

const SUBTYPE_LABELS: Record<string, string> = {
  CONTENT_CREATED: "Контент",
  CONTENT_UPDATED: "Контент обновлен",
  CONTENT_DELETED: "Контент удален",

  QA_NEW_QUESTION: "Вопрос",
  QA_ANSWERED: "Ответ",
  QA_ANSWER_TO_ANSWER: "Ответ",

  SESSION_SCHEDULED: "Сессия запланирована",
  SESSION_STARTED: "Сессия началась",
  SESSION_ENDED: "Сессия завершена",
};

const mapNotif = (b: BackendNotification): Notification => {
  const humanReadableSubtype = SUBTYPE_LABELS[b.eventCode] || b.eventCode;

  return {
    id: String(b.id),
    type: TYPE_MAP[b.type] || "content",
    subtype: humanReadableSubtype,
    title: b.title,
    body: b.body,
    route: b.route || undefined,
    time: b.createdAt,
    read: b.readAt !== null,
  };
};

const mapSettingsToFE = (data: BackendPreference[]): NotificationSetting[] => {
  const res: Record<string, NotificationSetting> = {};
  for (const p of data) {
    const key = EVENT_TO_KEY[p.eventCode];
    if (!key) continue;
    res[key] ||= { key, polling: false, push: false };
    if (p.channel === "POLLING") res[key].polling = p.enabled;
    if (p.channel === "PUSH") res[key].push = p.enabled;
  }
  return Object.values(res);
};

const mapSettingsToBE = (
  settings: NotificationSetting[],
): BackendPreference[] =>
  settings.flatMap((s) => [
    { eventCode: KEY_TO_EVENT[s.key], channel: "POLLING", enabled: s.polling },
    { eventCode: KEY_TO_EVENT[s.key], channel: "PUSH", enabled: s.push },
  ]);

/* ─── Server Actions ──────────────────────────────────────────────────── */

export async function getUnreadCount(roomName: string): Promise<number> {
  const [error, data] = await getWithAuth(
    `/notifications/${roomName}/unread-count`,
  );
  if (error) console.error(error);
  return data ?? 0;
}

export async function getNotifications(
  roomName: string,
): Promise<Notification[]> {
  const [error, data] = await getWithAuth(`/notifications/${roomName}`);
  if (error) console.error(error);
  return (data as BackendNotification[]).map(mapNotif);
}

export async function markNotificationRead(
  roomName: string,
  id: string,
): Promise<void> {
  const [error] = await sendWithAuth(
    `/notifications/${roomName}/${id}/read`,
    "POST",
  );
  if (error) console.error(error);
}

export async function dismissNotification(
  roomName: string,
  id: string,
): Promise<void> {
  const [error] = await sendWithAuth(
    `/notifications/${roomName}/${id}`,
    "DELETE",
  );
  if (error) console.error(error);
}

export async function markAllNotificationsRead(
  roomName: string,
): Promise<void> {
  const [error] = await sendWithAuth(
    `/notifications/${roomName}/read-all`,
    "POST",
  );
  if (error) console.error(error);
}

export async function clearAllNotifications(roomName: string): Promise<void> {
  const [error] = await sendWithAuth(
    `/notifications/${roomName}/all`,
    "DELETE",
  );
  if (error) console.error(error);
}

export async function getNotificationSettings(
  roomName: string,
): Promise<NotificationSetting[]> {
  const [error, data] = await getWithAuth(
    `/notifications/${roomName}/preferences`,
  );
  if (error) console.error(error);
  return mapSettingsToFE(data as BackendPreference[]);
}

export async function saveNotificationSettings(
  roomName: string,
  settings: NotificationSetting[],
): Promise<void> {
  const [error] = await sendWithAuth(
    `/notifications/${roomName}/preferences`,
    "POST",
    mapSettingsToBE(settings),
  );
  if (error) console.error(error);
}
