"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Bell,
  Edit,
  LogOut,
  Check,
  ChevronRight,
  X,
  Settings,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { cn } from "@/lib/utils";
import { NavRoom, RoomSection } from "@/types/room";
import { ThemeToggle } from "./theme-toggle";
import { MobileNotificationPanel } from "./mobile-notification-bell";
import { ChangeNameModal } from "./change-name-modal";
import CustomLink from "../no-prefetch-link";

interface MoreSheetProps {
  open: boolean;
  onClose: () => void;
  room: NavRoom;
  allRooms: NavRoom[];
  moreSections: RoomSection[];
  onOpenSettings: (open: boolean) => void;
  currentSection: string;
  userName: string;
  onNameChange: (newName: string) => void;
  unreadCount: number;
}

const DRAG_CLOSE_THRESHOLD = 80;

export function MoreSheet({
  open,
  onClose,
  room,
  allRooms,
  moreSections,
  currentSection,
  userName,
  unreadCount,
  onNameChange,
  onOpenSettings,
}: Readonly<MoreSheetProps>) {
  const router = useRouter();
  const [mounted, setMounted] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);

  const dragStartY = useRef<number | null>(null);
  const [dragOffset, setDragOffset] = useState(0);
  const sheetRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setMounted(true);
  }, []);

  // Закрытие по Escape
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        e.preventDefault();
        onClose();
      }
    };
    globalThis.addEventListener("keydown", onKey);
    return () => globalThis.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  // Сброс драга при закрытии
  useEffect(() => {
    if (!open) setDragOffset(0);
  }, [open]);

  const initials = userName
    ? userName
        .split(" ")
        .map((n) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)
    : "??";

  if (!open && dragOffset === 0) return null;

  const handleAction = (fn: () => void) => {
    fn();
    onClose();
  };

  // Drag handlers
  const onDragStart = (clientY: number) => {
    dragStartY.current = clientY;
  };

  const onDragMove = (clientY: number) => {
    if (dragStartY.current === null) return;
    const delta = clientY - dragStartY.current;
    if (delta > 0) setDragOffset(delta);
  };

  const onDragEnd = (clientY: number) => {
    if (dragStartY.current === null) return;
    const delta = clientY - dragStartY.current;
    dragStartY.current = null;

    if (delta >= DRAG_CLOSE_THRESHOLD) {
      setDragOffset(0);
      onClose();
    } else {
      setDragOffset(0);
    }
  };

  const onTouchStart = (e: React.TouchEvent) =>
    onDragStart(e.touches[0].clientY);
  const onTouchMove = (e: React.TouchEvent) => {
    e.stopPropagation();
    onDragMove(e.touches[0].clientY);
  };
  const onTouchEnd = (e: React.TouchEvent) =>
    onDragEnd(e.changedTouches[0].clientY);

  const onPointerDown = (e: React.PointerEvent) => {
    e.currentTarget.setPointerCapture(e.pointerId);
    onDragStart(e.clientY);
  };
  const onPointerMove = (e: React.PointerEvent) => {
    e.stopPropagation();
    onDragMove(e.clientY);
  };
  const onPointerUp = (e: React.PointerEvent) => {
    e.currentTarget.releasePointerCapture(e.pointerId);
    onDragEnd(e.clientY);
  };

  return (
    <>
      {/* Backdrop */}
      <div
        className={cn(
          "fixed inset-0 z-30 bg-black/40 backdrop-blur-sm transition-opacity duration-300",
          open ? "opacity-100" : "opacity-0 pointer-events-none",
        )}
        onClick={(e) => {
          e.stopPropagation();
          onClose();
        }}
        aria-hidden="true"
      />

      {/* Sheet */}
      <div
        ref={sheetRef}
        className={cn(
          "fixed bottom-0 left-0 right-0 z-40 rounded-t-2xl bg-card border-t border-border shadow-2xl",
          "transition-transform duration-300 ease-out will-change-transform",
          open || dragOffset > 0
            ? "translate-y-0"
            : "translate-y-[calc(100%-2rem)]",
        )}
        style={{
          transform: dragOffset > 0 ? `translateY(${dragOffset}px)` : undefined,
          transition: dragOffset > 0 ? "none" : undefined,
        }}
      >
        {/* Drag handle — touch-none so only this area drives the drag gesture */}
        <div
          className="flex justify-center pt-3 pb-2 cursor-grab active:cursor-grabbing touch-none select-none"
          onTouchStart={onTouchStart}
          onTouchMove={onTouchMove}
          onTouchEnd={onTouchEnd}
          onPointerDown={onPointerDown}
          onPointerMove={onPointerMove}
          onPointerUp={onPointerUp}
          aria-hidden="true"
        >
          <div className="w-10 h-1.5 rounded-full bg-muted-foreground/30" />
        </div>

        {/* touch-pan-y lets native scroll work inside the sheet */}
        <div
          className="max-h-[80vh] overflow-y-auto overscroll-contain touch-pan-y mb-4"
          style={
            {
              paddingBottom: "max(2rem, env(safe-area-inset-bottom))",
              WebkitOverflowScrolling: "touch",
            } as React.CSSProperties
          }
        >
          {/* Profile Header */}
          <div className="flex items-center justify-between gap-3 px-4 py-3 border-b border-border sticky top-0 bg-card z-10">
            <div className="flex items-center gap-3">
              <Avatar className="h-10 w-10 shrink-0">
                <AvatarFallback className="bg-primary text-primary-foreground text-sm font-semibold">
                  {initials}
                </AvatarFallback>
              </Avatar>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold truncate">{userName}</p>
                <p className="text-xs text-muted-foreground truncate">
                  {room.name}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-1">
              <ThemeToggle />
              <button
                onClick={onClose}
                className="p-2 rounded-full hover:bg-accent text-muted-foreground hover:text-foreground transition-colors md:hidden"
                aria-label="Закрыть"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          </div>

          {/* Rooms */}
          {allRooms.length > 1 && (
            <div className="px-2 pt-3 pb-1">
              <p className="px-2 mb-1 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                Комнаты
              </p>
              {allRooms.map((r) => (
                <Link
                  key={r.id}
                  href={`/rooms/${r.id}-${r.slug}`}
                  onClick={onClose}
                  className={cn(
                    "flex items-center gap-3 px-3 py-2.5 rounded-xl transition-colors",
                    r.id === room.id
                      ? "bg-primary/10 text-primary font-semibold"
                      : "text-foreground hover:bg-accent",
                  )}
                >
                  <span
                    className={cn(
                      "flex items-center justify-center w-7 h-7 rounded-lg text-xs font-bold shrink-0",
                      r.id === room.id
                        ? "bg-primary text-primary-foreground"
                        : "bg-muted text-muted-foreground",
                    )}
                  >
                    {r.name.slice(0, 2).toUpperCase()}
                  </span>
                  <span className="text-sm flex-1 truncate">{r.name}</span>
                  {r.id === room.id && (
                    <Check className="h-4 w-4 text-primary shrink-0" />
                  )}
                </Link>
              ))}
            </div>
          )}

          {/* Sections */}
          <div className="px-2 pt-3 pb-1">
            <p className="px-2 mb-1 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
              Разделы
            </p>
            {moreSections.map((section) => {
              const isActive = section.id === currentSection;
              const hasQBadge =
                section.id === "questions" &&
                room.isAdmin &&
                (room.questionCount ?? 0) > 0;

              return (
                <CustomLink
                  key={section.id}
                  href={section.href(room.id + "-" + room.slug)}
                  onClick={onClose}
                  className={cn(
                    "flex items-center gap-3 px-3 py-2.5 rounded-xl transition-colors",
                    isActive
                      ? "bg-primary/10 text-primary font-semibold"
                      : "text-foreground hover:bg-accent",
                  )}
                >
                  <span
                    className={cn(
                      "shrink-0",
                      isActive ? "text-primary" : "text-muted-foreground",
                    )}
                  >
                    {section?.icon}
                  </span>
                  <span className="text-sm flex-1">{section?.label}</span>
                  {hasQBadge && (
                    <Badge className="ml-auto h-5 px-1.5 text-[10px]">
                      {room.questionCount}
                    </Badge>
                  )}
                  {isActive && (
                    <ChevronRight className="h-4 w-4 opacity-50 shrink-0" />
                  )}
                </CustomLink>
              );
            })}
          </div>

          {/* Actions */}
          <div className="px-2 pt-3 pb-4">
            <p className="px-2 mb-1 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
              Действия
            </p>

            {/* Notification panel — inline, toggled by the button below */}
            {notifOpen && (
              <div className="rounded-xl border border-border overflow-hidden mb-1">
                <MobileNotificationPanel onClose={() => setNotifOpen(false)} />
              </div>
            )}

            {!notifOpen && (
              <button
                onClick={() => setNotifOpen(true)}
                className="flex items-center gap-3 w-full px-3 py-2.5 rounded-xl hover:bg-accent transition-colors text-left"
              >
                <Bell className="h-5 w-5 text-muted-foreground shrink-0" />
                <span className="text-sm flex-1">Уведомления</span>
                {unreadCount > 0 && (
                  <Badge className="h-5 px-1.5 text-[10px]">
                    {unreadCount}
                  </Badge>
                )}
              </button>
            )}

            <button
              onClick={() => handleAction(() => onOpenSettings(true))}
              className="flex items-center gap-3 w-full px-3 py-2.5 rounded-xl hover:bg-accent transition-colors text-left"
            >
              <Settings className="h-5 w-5 text-muted-foreground shrink-0" />
              <span className="text-sm flex-1">Настройки уведомлений</span>
            </button>

            <button
              onClick={() => setIsEditDialogOpen(true)}
              className="flex items-center gap-3 w-full px-3 py-2.5 rounded-xl hover:bg-accent transition-colors text-left"
            >
              <Edit className="h-5 w-5 text-muted-foreground shrink-0" />
              <span className="text-sm flex-1">Сменить имя</span>
            </button>

            <button
              onClick={() =>
                handleAction(() => {
                  if (mounted) router.push("/");
                })
              }
              className="flex items-center gap-3 w-full px-3 py-2.5 rounded-xl hover:bg-accent transition-colors text-left"
            >
              <LogOut className="h-5 w-5 text-muted-foreground shrink-0" />
              <span className="text-sm flex-1 text-destructive">
                К комнатам
              </span>
            </button>
          </div>
        </div>
      </div>
      <ChangeNameModal
        isOpen={isEditDialogOpen}
        onClose={setIsEditDialogOpen}
        currentName={userName}
        onNameUpdate={onNameChange}
      />
    </>
  );
}
