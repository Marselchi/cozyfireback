"use client";

import Link from "next/link";
import { MoreHorizontal } from "lucide-react";
import { cn } from "@/lib/utils";
import { NavRoom, RoomSection } from "@/types/room";
import CustomLink from "../no-prefetch-link";

interface MobileBottomNavProps {
  room: NavRoom;
  currentSection: string;
  mobileTabs: RoomSection[];
  moreOpen: boolean;
  isMoreActive: boolean;
  unreadCount: number;
  onMoreOpen: () => void;
  visible: boolean;
}

export function MobileBottomNav({
  room,
  currentSection,
  mobileTabs,
  moreOpen,
  isMoreActive,
  unreadCount,
  onMoreOpen,
  visible,
}: Readonly<MobileBottomNavProps>) {
  return (
    <nav
      aria-label="Mobile navigation"
      className={cn(
        "fixed bottom-0 left-0 right-0 z-50 md:hidden",
        "bg-card/95 backdrop-blur supports-backdrop-filter:bg-card/90",
        "border-t border-border",
        "transition-transform duration-300 ease-in-out",
        "touch-pan-y",
        visible ? "translate-y-0" : "translate-y-full",
      )}
      style={{ paddingBottom: "env(safe-area-inset-bottom)" }}
      aria-hidden={!visible}
    >
      <div className="flex items-stretch h-16">
        {mobileTabs.map((section) => {
          const isActive = section.id === currentSection;
          const hasQBadge =
            section.id === "questions" &&
            room.isAdmin &&
            (room.questionCount ?? 0) > 0;

          return (
            <CustomLink
              key={section.id}
              href={section.href(room.id + "-" + room.slug)}
              className={cn(
                "flex-1 flex flex-col items-center justify-center gap-0.5 relative",
                "transition-colors select-none active:bg-accent/50",
                isActive ? "text-primary" : "text-muted-foreground",
              )}
              aria-label={section?.label}
              aria-current={isActive ? "page" : undefined}
            >
              {isActive && (
                <span className="absolute inset-x-3 top-2 bottom-2 rounded-xl bg-primary/10 -z-1" />
              )}
              <span className="relative">
                {section?.icon}
                {hasQBadge && (
                  <span className="absolute -top-1 -right-2 flex items-center justify-center min-w-4 h-4 px-1 rounded-full bg-primary text-primary-foreground text-[9px] font-bold leading-none">
                    {(room.questionCount ?? 0) > 9 ? "9+" : room.questionCount}
                  </span>
                )}
              </span>
              <span className="text-[10px] font-medium leading-none">
                {section?.shortLabel}
              </span>
            </CustomLink>
          );
        })}

        <button
          type="button"
          onClick={onMoreOpen}
          className={cn(
            "flex-1 flex flex-col items-center justify-center gap-0.5 relative",
            "transition-colors select-none active:bg-accent/50",
            isMoreActive || moreOpen ? "text-primary" : "text-muted-foreground",
          )}
          aria-label="Ещё"
          aria-expanded={moreOpen}
        >
          {(isMoreActive || moreOpen) && (
            <span className="absolute inset-x-3 top-2 bottom-2 rounded-xl bg-primary/10 -z-1" />
          )}
          <span className="relative">
            <MoreHorizontal className="h-5 w-5" />
            {unreadCount > 0 && (
              <span className="absolute -top-1 -right-2 flex items-center justify-center min-w-4 h-4 px-1 rounded-full bg-primary text-primary-foreground text-[9px] font-bold leading-none">
                {unreadCount > 9 ? "9+" : unreadCount}
              </span>
            )}
          </span>
          <span className="text-[10px] font-medium leading-none">Ещё</span>
        </button>
      </div>
    </nav>
  );
}
