"use client";

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { formatDistanceToNow } from "date-fns";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import {
  Bell,
  FileText,
  HelpCircle,
  CheckCheck,
  Trash2,
  X,
  ExternalLink,
  Calendar,
  RefreshCw,
} from "lucide-react";
import {
  Notification,
  NotifType,
  getUnreadCount,
  getNotifications,
  markNotificationRead,
  dismissNotification,
  markAllNotificationsRead,
  clearAllNotifications,
} from "@/server/notifications/notifications";
import { ru } from "date-fns/locale";
import { useRoomId } from "@/lib/room-utils";

/* ─── Query keys ─────────────────────────────────────────────────────────── */
export const NOTIF_KEYS = {
  unreadCount: ["notifications", "unread-count"] as const,
  list: ["notifications", "list"] as const,
};

/* ─── Type config ────────────────────────────────────────────────────────── */
const TYPE_CONFIG: Record<
  NotifType,
  { icon: React.ReactNode; colorClass: string; bgClass: string }
> = {
  content: {
    icon: <FileText className="w-4 h-4" />,
    colorClass: "text-blue-600 dark:text-blue-400",
    bgClass: "bg-blue-500/10",
  },
  qa: {
    icon: <HelpCircle className="w-4 h-4" />,
    colorClass: "text-violet-600 dark:text-violet-400",
    bgClass: "bg-violet-500/10",
  },
  session: {
    icon: <Calendar className="w-4 h-4" />,
    colorClass: "text-teal-600 dark:text-teal-400",
    bgClass: "bg-teal-500/10",
  },
};

/* ─── NotifItem ──────────────────────────────────────────────────────────── */
function NotifItem({
  notif,
  onRead,
  onDismiss,
}: Readonly<{
  notif: Notification;
  onRead: (id: string) => void;
  onDismiss: (id: string) => void;
}>) {
  const router = useRouter();
  const cfg = TYPE_CONFIG[notif.type];
  const roomName = useRoomId();

  return (
    <li
      className={cn(
        "relative flex gap-3 px-4 py-4 transition-colors border-b border-border/60 last:border-b-0",
        !notif.read ? "bg-primary/5" : "",
      )}
    >
      <div className="flex-1 min-w-0">
        {/* Type badge + time */}
        <div className="flex items-start justify-between gap-2">
          <span
            className={cn(
              "inline-flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wider px-2 py-1 rounded-md",
              cfg.bgClass,
              cfg.colorClass,
            )}
          >
            {cfg.icon}
            {notif.subtype}
            {!notif.read && (
              <span className="w-1.5 h-1.5 rounded-full bg-current inline-block ml-0.5" />
            )}
          </span>
          <span className="text-xs text-muted-foreground whitespace-nowrap shrink-0 mt-0.5">
            {formatDistanceToNow(new Date(notif.time), {
              addSuffix: true,
              locale: ru,
            })}
          </span>
        </div>

        <p className="text-sm font-semibold text-foreground mt-2 leading-snug">
          {notif.title}
        </p>
        <p className="text-sm text-muted-foreground mt-1 leading-relaxed line-clamp-3">
          {notif.body}
        </p>

        {/* Action row */}
        <div className="flex items-center gap-2 mt-3">
          {notif.route && (
            <Button
              variant="outline"
              size="sm"
              className="h-8 px-3 text-xs gap-1.5"
              onClick={() => router.push(`/rooms/${roomName}${notif.route!}`)}
            >
              <ExternalLink className="w-3.5 h-3.5" />
              Перейти
            </Button>
          )}
          {!notif.read && (
            <button
              onClick={() => onRead(notif.id)}
              className="flex items-center gap-1.5 h-8 px-3 rounded-md text-xs text-muted-foreground hover:text-foreground hover:bg-accent transition-colors border border-border"
              aria-label="Отметить прочитанным"
            >
              <CheckCheck className="w-3.5 h-3.5" />
              Прочитано
            </button>
          )}
          <button
            onClick={() => onDismiss(notif.id)}
            className="ml-auto flex items-center justify-center w-8 h-8 rounded-md text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors"
            aria-label="Скрыть уведомление"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>
    </li>
  );
}

/* ─── MobileNotificationPanel ────────────────────────────────────────────── */
/**
 * Self-contained notification panel meant to be embedded directly inside
 * a sheet/drawer (e.g. MoreSheet). It has no trigger of its own — the
 * parent controls visibility.
 *
 * Props:
 *   onClose — called when the user taps the close (X) button in the header.
 */
export function MobileNotificationPanel({
  onClose,
}: Readonly<{
  onClose: () => void;
}>) {
  const [tab, setTab] = useState<"all" | "unread">("all");
  const queryClient = useQueryClient();
  const roomName = useRoomId();
  /* Unread count — polled every 60 s */
  const { data: unreadCount = 0 } = useQuery({
    queryKey: NOTIF_KEYS.unreadCount,
    queryFn: () => getUnreadCount(roomName),
    enabled: false, //disable calls until fix
    refetchInterval: 60_000,
  });

  /* Full list — always fetched because the panel is embedded (already visible) */
  const { data: notifs = [] } = useQuery({
    queryKey: NOTIF_KEYS.list,
    queryFn: () => getNotifications(roomName),
  });

  const markRead = useMutation({
    mutationFn: (id: string) => markNotificationRead(roomName, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: NOTIF_KEYS.list });
      queryClient.invalidateQueries({ queryKey: NOTIF_KEYS.unreadCount });
    },
  });

  const dismiss = useMutation({
    mutationFn: (id: string) => dismissNotification(roomName, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: NOTIF_KEYS.list });
      queryClient.invalidateQueries({ queryKey: NOTIF_KEYS.unreadCount });
    },
  });

  const markAll = useMutation({
    mutationFn: () => markAllNotificationsRead(roomName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: NOTIF_KEYS.list });
      queryClient.invalidateQueries({ queryKey: NOTIF_KEYS.unreadCount });
    },
  });

  const clearAll = useMutation({
    mutationFn: () => clearAllNotifications(roomName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: NOTIF_KEYS.list });
      queryClient.invalidateQueries({ queryKey: NOTIF_KEYS.unreadCount });
    },
  });

  const displayed = tab === "unread" ? notifs.filter((n) => !n.read) : notifs;

  return (
    <div className="flex flex-col w-full bg-card">
      {/* ── Header ── */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-border">
        <div className="flex items-center gap-2.5">
          <Bell className="w-5 h-5 text-muted-foreground" />
          <h2 className="text-base font-semibold text-foreground">
            Уведомления
          </h2>
          {unreadCount > 0 && (
            <Badge
              variant="secondary"
              className="text-xs px-1.5 py-0 h-5 font-semibold bg-primary/10 text-primary border-primary/20"
            >
              {unreadCount}
            </Badge>
          )}
        </div>

        <div className="flex items-center gap-1">
          <button
            onClick={() => {
              queryClient.invalidateQueries({ queryKey: NOTIF_KEYS.list });
              queryClient.invalidateQueries({
                queryKey: NOTIF_KEYS.unreadCount,
              });
            }}
            className="w-9 h-9 flex items-center justify-center rounded-full text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
            aria-label="Обновить"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
          <button
            onClick={onClose}
            className="w-9 h-9 flex items-center justify-center rounded-full text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
            aria-label="Закрыть уведомления"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* ── Tabs ── */}
      <div className="px-4 pt-3 pb-2">
        <Tabs value={tab} onValueChange={(v) => setTab(v as "all" | "unread")}>
          <TabsList className="w-full h-10 p-1 bg-muted/60">
            <TabsTrigger
              value="all"
              className="flex-1 h-8 text-sm data-[state=active]:bg-background data-[state=active]:text-foreground data-[state=active]:shadow-sm"
            >
              Все
            </TabsTrigger>
            <TabsTrigger
              value="unread"
              className="flex-1 h-8 text-sm data-[state=active]:bg-background data-[state=active]:text-foreground data-[state=active]:shadow-sm"
            >
              Непрочитанные
              {unreadCount > 0 && (
                <span className="ml-1.5 text-[10px] font-bold bg-primary text-primary-foreground rounded-full px-1.5 py-0.5 leading-none">
                  {unreadCount}
                </span>
              )}
            </TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      {/* ── Bulk actions ── */}
      {unreadCount > 0 && (
        <div className="px-4 pb-2">
          <button
            onClick={() => markAll.mutate()}
            className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            <CheckCheck className="w-4 h-4" />
            Отметить все прочитанными
          </button>
        </div>
      )}

      {/* ── List — native scroll, no ScrollArea wrapper ── */}
      <div
        className="overflow-y-auto overscroll-auto"
        style={
          {
            maxHeight: "50vh",
            WebkitOverflowScrolling: "touch",
          } as React.CSSProperties
        }
      >
        {displayed.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-36 gap-3 text-muted-foreground">
            <Bell className="w-9 h-9 opacity-30" />
            <p className="text-sm">
              {tab === "unread"
                ? "Нет непрочитанных уведомлений"
                : "Нет уведомлений"}
            </p>
          </div>
        ) : (
          <ul aria-label="Уведомления">
            {displayed.map((n) => (
              <NotifItem
                key={n.id}
                notif={n}
                onRead={(id) => markRead.mutate(id)}
                onDismiss={(id) => dismiss.mutate(id)}
              />
            ))}
          </ul>
        )}
      </div>

      {/* ── Footer ── */}
      {notifs.length > 0 && (
        <div className="flex items-center justify-between px-4 py-3 border-t border-border bg-muted/30">
          <button
            onClick={() => clearAll.mutate()}
            className="flex items-center gap-2 text-sm text-muted-foreground hover:text-destructive transition-colors"
          >
            <Trash2 className="w-4 h-4" />
            Очистить все
          </button>
          <span className="text-xs text-muted-foreground">
            {notifs.length} уведомлений
          </span>
        </div>
      )}
    </div>
  );
}

/* ─── MobileNotificationBell ─────────────────────────────────────────────── */
/**
 * Bottom-nav trigger button only. Renders the bell icon with unread badge.
 * Tapping it should open whatever panel/sheet the parent controls.
 */
export function MobileNotificationBell({
  onOpen,
}: Readonly<{
  onOpen: () => void;
}>) {
  const roomName = useRoomId();

  const { data: unreadCount = 0 } = useQuery({
    queryKey: NOTIF_KEYS.unreadCount,
    queryFn: () => getUnreadCount(roomName),
    enabled: false, //disable calls until fix
    refetchInterval: 60_000,
  });

  return (
    <button
      onClick={onOpen}
      className={cn(
        "relative flex flex-col items-center justify-center gap-1 flex-1 py-2 px-1 transition-colors",
        "text-muted-foreground hover:text-foreground",
      )}
      aria-label={`Уведомления${unreadCount > 0 ? `: ${unreadCount} непрочитано` : ""}`}
    >
      <span className="relative">
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span
            className="absolute -top-1 -right-1.5 flex items-center justify-center min-w-4 h-4 px-1 rounded-full bg-primary text-primary-foreground text-[9px] font-bold leading-none"
            aria-hidden
          >
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </span>
      <span className="text-[10px] font-medium leading-none">Уведомления</span>
    </button>
  );
}
