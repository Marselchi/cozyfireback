"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { Home, Users, Tags, Settings, Blocks } from "lucide-react";

const navItems = [
  { href: "./main", segment: "main", label: "Главная", icon: Home },
  { href: "./roles", segment: "roles", label: "Роли", icon: Users },
  { href: "./tags", segment: "tags", label: "Теги", icon: Tags },
  {
    href: "./campaign",
    segment: "settings",
    label: "Настройки",
    icon: Settings,
  },
  { href: "./blocks", segment: "blocks", label: "Блоки", icon: Blocks },
];

export function AdminSubNav() {
  const pathname = usePathname();

  return (
    <nav className="border-b border-border bg-card">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-12 items-center justify-center gap-1 overflow-x-auto">
          {navItems.map((item) => {
            const lastSegment = pathname.split("/").pop() || "";
            const isActive = lastSegment === item.segment;
            const Icon = item.icon;

            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "relative flex items-center gap-2 px-4 py-2 text-sm font-medium transition-colors rounded-md",
                  "hover:bg-secondary hover:text-foreground",
                  "text-muted-foreground",
                )}
              >
                <Icon className="h-4 w-4" />
                <span className="whitespace-nowrap">{item.label}</span>
                {isActive && (
                  <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-accent-foreground" />
                )}
              </Link>
            );
          })}
        </div>
      </div>
    </nav>
  );
}
