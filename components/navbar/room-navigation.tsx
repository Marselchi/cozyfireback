"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useParams, usePathname, useRouter } from "next/navigation";
import { Eye, EyeOff, ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { NavRoom } from "@/types/room";
import { roomSections } from "@/lib/sections";
import { useUser } from "@/lib/contexts/user-context";
import { MobileBottomNav } from "./mobile-bottom-nav";
import { MoreSheet } from "./more-sheet";
import { NotificationBell } from "./notification-bell";
import { RoomSelector } from "./room-selector";
import { ThemeToggle } from "./theme-toggle";
import { UserProfileMenu } from "./user-profile-menu";
import { NotificationSubscriptionDialog } from "./notification-subscription-dialog";
import { useQuery } from "@tanstack/react-query";
import { getUnreadCount } from "@/server/notifications/notifications";
import { useRoomId } from "@/lib/room-utils";
import CustomLink from "../no-prefetch-link";

interface RoomNavigationProps {
  room: NavRoom;
  allRooms: NavRoom[];
}

const MOBILE_TAB_IDS = new Set(["lore", "questions", "characters", "sessions"]);
const MOBILE_MORE_IDS = new Set(["main", "admin"]);

export function RoomNavigation({
  room,
  allRooms,
}: Readonly<RoomNavigationProps>) {
  const [isNavVisible, setIsNavVisible] = useState(true);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [moreOpen, setMoreOpen] = useState(false);

  const [mobileNavVisible, setMobileNavVisible] = useState(true);
  const lastScrollY = useRef(0);
  const ticking = useRef(false);

  const pathname = usePathname();
  const router = useRouter();
  const { user, updateUserName } = useUser();

  const parts = pathname.split("/").filter(Boolean);
  const currentSection = parts[2] || "main";
  const activeSection = roomSections.find((s) => s.id === currentSection);

  const visibleSections = roomSections.filter(
    (s) => !s.adminOnly || room.isAdmin,
  );
  const mobileTabs = roomSections.filter(
    (s) => MOBILE_TAB_IDS.has(s.id) && (!s.adminOnly || room.isAdmin),
  );
  const moreSections = roomSections.filter(
    (s) => MOBILE_MORE_IDS.has(s.id) && (!s.adminOnly || room.isAdmin),
  );
  const isMoreActive = moreSections.some((s) => s.id === currentSection);
  const roomName = useRoomId();
  const { data: unreadCount = 0 } = useQuery({
    queryKey: ["notifications", "unread-count"],
    queryFn: () => getUnreadCount(roomName),
    enabled: false, //disable calls until fix
    refetchInterval: 60_000,
  });

  const handleScroll = useCallback(() => {
    if (!ticking.current) {
      requestAnimationFrame(() => {
        const current = window.scrollY;
        if (current > lastScrollY.current + 10 && current > 80) {
          setMobileNavVisible(false);
        } else if (current < lastScrollY.current - 10) {
          setMobileNavVisible(true);
        }
        lastScrollY.current = current;
        ticking.current = false;
      });
      ticking.current = true;
    }
  }, []);

  const handlePageTap = useCallback(
    (e: TouchEvent) => {
      if (mobileNavVisible) return;
      const x = e.touches[0]?.clientX ?? 0;
      const screenW = window.innerWidth;
      if (x > screenW * 0.3 && x < screenW * 0.7) {
        setMobileNavVisible(true);
      }
    },
    [mobileNavVisible],
  );

  useEffect(() => {
    globalThis.addEventListener("scroll", handleScroll, { passive: true });
    globalThis.addEventListener("touchstart", handlePageTap, { passive: true });
    return () => {
      globalThis.removeEventListener("scroll", handleScroll);
      globalThis.removeEventListener("touchstart", handlePageTap);
    };
  }, [handleScroll, handlePageTap]);

  useEffect(() => {
    document.documentElement.style.setProperty(
      "--navbar-height",
      isNavVisible ? "4rem" : "0px",
    );
  }, [isNavVisible]);

  useEffect(() => {
    setMoreOpen(false);
  }, [pathname]);

  return (
    <>
      {!isNavVisible && (
        <div className="fixed top-0 right-0 z-50 p-2 pointer-events-none">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setIsNavVisible(true)}
            className="flex items-center gap-2 bg-card-foreground/5 pointer-events-auto"
          >
            <EyeOff className="h-4 w-4" />
            <span className="font-medium">{room.name}</span>
            {activeSection && (
              <>
                <span className="text-muted-foreground">/</span>
                <span className="text-muted-foreground">
                  {activeSection.label}
                </span>
              </>
            )}
          </Button>
        </div>
      )}

      {isNavVisible && (
        <nav
          className="sticky top-0 z-50 hidden md:flex h-16 items-center justify-between border-b border-border bg-card/95 px-4 backdrop-blur supports-backdrop-filter:bg-card/80 lg:px-6"
          suppressHydrationWarning
        >
          <div className="flex items-center gap-2">
            <RoomSelector currentRoom={room} allRooms={allRooms} />
          </div>

          <div className="flex items-center gap-1">
            <Button
              variant="ghost"
              size="sm"
              className="gap-2"
              onClick={() => router.back()}
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Назад</span>
            </Button>
            {visibleSections.map((section) => {
              const isActive = section.id === currentSection;
              const hasQuestionBadge =
                section.id === "questions" &&
                room.isAdmin &&
                (room.questionCount ?? 0) > 0;

              return (
                <Button
                  key={section.id}
                  variant={isActive ? "secondary" : "ghost"}
                  size="sm"
                  asChild
                  className={cn(
                    "transition-colors relative",
                    isActive && "font-medium",
                  )}
                >
                  <CustomLink href={section.href(room.id + "-" + room.slug)}>
                    <span className="flex items-center gap-2">
                      {section.label}
                      {hasQuestionBadge && (
                        <Badge className="absolute -top-1 -right-2 h-4 w-4 p-0 flex items-center justify-center text-[9px] pointer-events-none z-[2]">
                          {room.questionCount}
                        </Badge>
                      )}
                    </span>
                  </CustomLink>
                </Button>
              );
            })}
          </div>

          <div className="flex items-center gap-1">
            <NotificationBell onOpenSettings={() => setSettingsOpen(true)} />
            <ThemeToggle />
            <UserProfileMenu
              userName={user.name}
              onNameChange={updateUserName}
            />
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setIsNavVisible(false)}
              className="h-9 w-9"
            >
              <Eye className="h-4 w-4" />
              <span className="sr-only">Спрятать навигацию</span>
            </Button>
          </div>
        </nav>
      )}

      <MobileBottomNav
        room={room}
        currentSection={currentSection}
        mobileTabs={mobileTabs}
        moreOpen={moreOpen}
        isMoreActive={isMoreActive}
        unreadCount={unreadCount}
        onMoreOpen={() => setMoreOpen(true)}
        visible={mobileNavVisible}
      />

      <MoreSheet
        open={moreOpen}
        onClose={() => setMoreOpen(false)}
        room={room}
        allRooms={allRooms}
        moreSections={moreSections}
        currentSection={currentSection}
        onOpenSettings={setSettingsOpen}
        userName={user.name}
        onNameChange={updateUserName}
        unreadCount={unreadCount}
      />

      <NotificationSubscriptionDialog
        open={settingsOpen}
        onOpenChange={setSettingsOpen}
      />
    </>
  );
}
