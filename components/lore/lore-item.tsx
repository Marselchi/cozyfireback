"use client";

import * as React from "react";
import Link from "next/link";
import { cn } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { Shield, Bookmark, EyeOff, Sparkles, RefreshCw } from "lucide-react";
import { IdName } from "@/types/springTypes";
import CustomLink from "../no-prefetch-link";
import GlowCard from "@/components/glow-card";

export interface LoreItemData {
  id: number;
  title: string;
  description: string;
  date: string;
  author: string;
  byAdmin: boolean;
  isViewed: boolean | null;
  nonPublic: boolean;
  //readLater: boolean
  tags: IdName[];
}

export interface LoreItemOptions {
  href?: string;
  onTagClick?: (tag: IdName) => void;
}

export interface LoreItemProps {
  data: LoreItemData;
  options?: LoreItemOptions;
}

export function LoreItem({ data, options }: LoreItemProps) {
  const href = options?.href || `./lore/${data.id}`;

  const handleTagClick = (e: React.MouseEvent, tag: IdName) => {
    e.preventDefault();
    e.stopPropagation();
    options?.onTagClick?.(tag);
  };

  const statusIndicators = [];

  if (data.nonPublic) {
    statusIndicators.push({
      key: "nonPublic",
      icon: EyeOff,
      label: "Приватно",
      className: "text-[hsl(var(--lore-admin))]",
    });
  }

  return (
    <CustomLink href={href} className="block group h-full">
      <GlowCard
        glowRadius={700}
        glowColor={
          data.isViewed === null
            ? "0, 119, 255"
            : data.isViewed === false
              ? "0, 255, 68"
              : "128, 128, 128"
        }
        className={cn(
          "rounded-(--radius) bg-card text-card-foreground",
          "h-full",
          "transition-all duration-300 ease-out",
          "hover:-translate-y-0.5",
          data.nonPublic && "bg-card/80",
          data.isViewed === null &&
            "[--glow-left-border:4px] border-l-4 border-l-[hsl(var(--lore-pinned))]",
          data.isViewed === false &&
            "[--glow-left-border:4px] border-l-4 border-l-[hsl(var(--lore-read-later))]",
        )}
      >
        <article className="h-full flex flex-col">
          <div className="flex flex-col flex-1 p-5">
            {/* Flexible content area that grows to fill space */}
            <div className="space-y-4 grow">
              {/* Header section with status indicators */}
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0 space-y-2">
                  {/* Title and viewed status */}
                  <div className="flex items-start gap-2">
                    <h3
                      className={cn(
                        "font-semibold text-lg leading-tight text-balance transition-colors",
                        data.isViewed === null && "font-bold",
                      )}
                    >
                      {data.title}
                    </h3>
                  </div>

                  {/* Description */}
                  <p className="text-sm text-muted-foreground leading-relaxed line-clamp-2 wrap-break-word">
                    {data.description}
                  </p>
                </div>

                {/* Status indicators column */}
                {statusIndicators.length > 0 && (
                  <div className="flex gap-1.5 shrink-0">
                    {statusIndicators.map(
                      ({ key, icon: Icon, label, className }) => (
                        <div
                          key={key}
                          className={cn(
                            "p-1.5 rounded-md bg-secondary/50 transition-all duration-200",
                            className,
                          )}
                          title={label}
                        >
                          <Icon className="h-3.5 w-3.5" />
                        </div>
                      ),
                    )}
                  </div>
                )}
              </div>

              {/* Tags section */}
              {data.tags.length > 0 && (
                <div className="flex flex-wrap gap-2 mb-1">
                  {data.tags.map((tag) => (
                    <button
                      key={tag.id}
                      onClick={(e) => handleTagClick(e, tag)}
                      className={cn(
                        "inline-flex items-center px-2.5 py-1 rounded-md text-xs font-medium",
                        "bg-secondary text-secondary-foreground",
                        "transition-all duration-200",
                        "hover:bg-current/10 hover:text-accent-foreground hover:scale-105",
                        "focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2",
                        "active:scale-95",
                      )}
                    >
                      {tag.name}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Footer section - stays at the bottom */}
            <div className="pt-2 border-t border-input mt-auto">
              <div className="flex items-center justify-between gap-4">
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  {data.byAdmin && (
                    <Badge
                      variant="outline"
                      className="border-[hsl(var(--lore-admin))] text-[hsl(var(--lore-admin))] px-2 py-0.5"
                    >
                      <Shield className="h-3 w-3 mr-1" />
                      DM
                    </Badge>
                  )}
                  <span className="font-medium">{data.author}</span>
                </div>

                <div className="flex items-center gap-3 text-xs text-muted-foreground">
                  <span>{data.date}</span>
                  {data.isViewed === null && (
                    <span className="flex items-center gap-1 text-[hsl(var(--lore-pinned))]  font-medium">
                      <Sparkles className="h-3 w-3" />
                      Не прочитано
                    </span>
                  )}
                  {data.isViewed === false && (
                    <span className="flex items-center gap-1 text-[hsl(var(--lore-read-later))] font-medium">
                      <RefreshCw className="h-3 w-3" />
                      Обновлено
                    </span>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Hover effect overlay */}
          <div
            className={cn(
              "absolute inset-0 bg-linear-to-r from-accent/0 via-accent/0 to-accent/0",
              "opacity-0 group-hover:opacity-[0.03] transition-opacity duration-300",
              "pointer-events-none",
            )}
          />
        </article>
      </GlowCard>
    </CustomLink>
  );
}
