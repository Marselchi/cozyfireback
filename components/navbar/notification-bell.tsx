"use client";

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { formatDistanceToNow } from "date-fns";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import {
  Bell,
  FileText,
  HelpCircle,
  Settings,
  CheckCheck,
  Trash2,
  X,
  Dot,
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
import { registerPush } from "@/lib/registerPush";
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
    icon: <FileText className="w-3.5 h-3.5" />,
    colorClass: "text-blue-600 dark:text-blue-400",
    bgClass: "bg-blue-500/10",
  },
  qa: {
    icon: <HelpCircle className="w-3.5 h-3.5" />,
    colorClass: "text-violet-600 dark:text-violet-400",
    bgClass: "bg-violet-500/10",
  },
  session: {
    icon: <Calendar className="w-3.5 h-3.5" />,
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

  return (
    <li
      className={cn(
        "group relative flex gap-3 px-4 py-3 transition-colors border-b border-border/60 last:border-b-0",
        !notif.read ? "bg-primary/3 hover:bg-primary/6" : "hover:bg-accent/50",
      )}
    >
      {/* Content */}
      <div className="flex-1 min-w-0">
        <div className="flex items-start justify-between gap-2">
          <span
            className={cn(
              "inline-flex items-center gap-1 text-[10px] font-semibold uppercase tracking-wider px-1.5 py-0.5 rounded",
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
          <span className="text-[10px] text-muted-foreground whitespace-nowrap shrink-0 mt-0.5">
            {formatDistanceToNow(new Date(notif.time), {
              addSuffix: true,
              locale: ru,
            })}
          </span>
        </div>

        <p className="text-xs font-semibold text-foreground mt-1 leading-snug">
          {notif.title}
        </p>
        <p className="text-xs text-muted-foreground mt-0.5 leading-relaxed line-clamp-2">
          {notif.body}
        </p>

        {/* Go to button */}
        {notif.route && (
          <Button
            variant="ghost"
            size="sm"
            className="mt-1.5 h-6 px-2 text-[11px] text-muted-foreground hover:text-foreground gap-1"
            onClick={() => router.push(`/rooms${notif.route!}`)}
          >
            <ExternalLink className="w-3 h-3" />
            Перейти
          </Button>
        )}
      </div>

      {/* Hover actions */}
      <div className="absolute right-3 bottom-3 hidden group-hover:flex items-center gap-1">
        {!notif.read && (
          <button
            onClick={() => onRead(notif.id)}
            title="Mark as read"
            className="p-1 rounded-md text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
            aria-label="Mark as read"
          >
            <CheckCheck className="w-3.5 h-3.5" />
          </button>
        )}
        <button
          onClick={() => onDismiss(notif.id)}
          title="Dismiss"
          className="p-1 rounded-md text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors"
          aria-label="Dismiss notification"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>
    </li>
  );
}

/* ─── Main export ────────────────────────────────────────────────────────── */
export function NotificationBell({
  onOpenSettings,
}: Readonly<{
  onOpenSettings: () => void;
}>) {
  const [open, setOpen] = useState(false);
  const [tab, setTab] = useState<"all" | "unread">("all");
  const queryClient = useQueryClient();
  const roomName = useRoomId();

  /* Polling: unread count every 60 seconds */
  const { data: unreadCount = 0 } = useQuery({
    queryKey: NOTIF_KEYS.unreadCount,
    queryFn: () => getUnreadCount(roomName),
    enabled: false, //disable calls until fix
    refetchInterval: 60_000,
  });

  /* Fetch full list only when popover is open */
  const { data: notifs = [] } = useQuery({
    queryKey: NOTIF_KEYS.list,
    queryFn: () => getNotifications(roomName),
    enabled: open,
  });

  /* Mutations */
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
    mutationFn: markAllNotificationsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: NOTIF_KEYS.list });
      queryClient.invalidateQueries({ queryKey: NOTIF_KEYS.unreadCount });
    },
  });

  const clearAll = useMutation({
    mutationFn: clearAllNotifications,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: NOTIF_KEYS.list });
      queryClient.invalidateQueries({ queryKey: NOTIF_KEYS.unreadCount });
    },
  });

  const displayed = tab === "unread" ? notifs.filter((n) => !n.read) : notifs;

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <button
          className={cn(
            "relative flex items-center justify-center w-9 h-9 rounded-lg transition-colors",
            "text-muted-foreground hover:text-foreground hover:bg-accent",
            open && "bg-accent text-foreground",
          )}
          aria-label={`Уведомления${unreadCount > 0 ? `: ${unreadCount} непрочитано` : ""}`}
        >
          <Bell className="w-5 h-5" />
          {unreadCount > 0 && (
            <span
              className="absolute top-1.5 right-1.5 flex items-center justify-center min-w-4 h-4 px-1 rounded-full bg-primary text-primary-foreground text-[9px] font-bold leading-none"
              aria-hidden
            >
              {unreadCount > 9 ? "9+" : unreadCount}
            </span>
          )}
        </button>
      </PopoverTrigger>

      <PopoverContent
        align="end"
        sideOffset={8}
        className="w-100 p-0 rounded-xl border border-border shadow-xl bg-popover overflow-hidden"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-4 pt-4 pb-3 border-b border-border">
          <div className="flex items-center gap-2">
            <h2 className="text-sm font-semibold text-foreground">
              Уведомления
            </h2>
            {unreadCount > 0 && (
              <Badge
                variant="secondary"
                className="text-[10px] px-1.5 py-0 h-4 font-semibold bg-primary/10 text-primary border-primary/20"
              >
                {unreadCount}
              </Badge>
            )}
          </div>
          <div className="flex items-center gap-1">
            {unreadCount > 0 && (
              <button
                onClick={() => markAll.mutate(roomName)}
                className="text-[11px] text-muted-foreground hover:text-foreground flex items-center gap-1 px-2 py-1 rounded-md hover:bg-accent transition-colors"
              >
                <CheckCheck className="w-3 h-3" />
                Прочитать все
              </button>
            )}
            <button
              onClick={() => {
                queryClient.invalidateQueries({ queryKey: NOTIF_KEYS.list });
                queryClient.invalidateQueries({
                  queryKey: NOTIF_KEYS.unreadCount,
                });
              }}
              className="p-1.5 rounded-md text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
              aria-label="Обновить уведомления"
              title="Обновить"
            >
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
            <button
              onClick={() => {
                registerPush();
                setOpen(false);
                onOpenSettings();
              }}
              className="p-1.5 rounded-md text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
              aria-label="Notification settings"
              title="Notification settings"
            >
              <Settings className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Tabs */}
        <div className="px-4 pt-2 pb-1 border-b border-border">
          <Tabs
            value={tab}
            onValueChange={(v) => setTab(v as "all" | "unread")}
          >
            <TabsList className="h-7 p-0.5 gap-1 bg-muted/50">
              <TabsTrigger
                value="all"
                className="h-6 text-xs px-3 data-[state=active]:bg-background data-[state=active]:text-foreground"
              >
                Все
              </TabsTrigger>
              <TabsTrigger
                value="unread"
                className="h-6 text-xs px-3 data-[state=active]:bg-background data-[state=active]:text-foreground"
              >
                Непрочитанные
                {unreadCount > 0 && (
                  <span className="ml-1.5 text-[9px] font-bold bg-primary text-primary-foreground rounded-full px-1 py-0.5 leading-none">
                    {unreadCount}
                  </span>
                )}
              </TabsTrigger>
            </TabsList>
          </Tabs>
        </div>

        {/* List */}
        <ScrollArea className="h-105">
          {displayed.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-40 gap-2 text-muted-foreground">
              <Bell className="w-8 h-8 opacity-30" />
              <p className="text-sm">
                {tab === "unread"
                  ? "Нет непрочитанных уведомлений"
                  : "Нет уведомлений"}
              </p>
            </div>
          ) : (
            <ul aria-label="Notifications">
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
        </ScrollArea>

        {/* Footer */}
        {notifs.length > 0 && (
          <div className="flex items-center justify-between px-4 py-2.5 border-t border-border bg-muted/30">
            <button
              onClick={() => clearAll.mutate(roomName)}
              className="text-[11px] text-muted-foreground hover:text-destructive flex items-center gap-1 transition-colors"
            >
              <Trash2 className="w-3 h-3" />
              Очистить все
            </button>
            <span className="text-[11px] text-muted-foreground">
              {notifs.length} уведомлений
            </span>
          </div>
        )}
      </PopoverContent>
    </Popover>
  );
}
