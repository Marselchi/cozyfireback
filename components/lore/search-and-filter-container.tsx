"use client";

import type React from "react";

import { useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Plus, Search, X } from "lucide-react";
import { Toggle } from "@/components/ui/toggle";
import CustomLink from "../no-prefetch-link";
import { IdName } from "@/types/springTypes";

interface SearchProps {
  tags: IdName[];
}

export default function SearchAndFilterContainer({
  tags,
}: Readonly<SearchProps>) {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [searchQuery, setSearchQuery] = useState(
    searchParams.get("search") ?? "",
  );
  const [selectedTag, setSelectedTag] = useState(searchParams.get("tag") ?? "");
  const [adminFilter, setAdminFilter] = useState(
    searchParams.get("admin") ?? "",
  );
  const [draftsMode, setDraftsMode] = useState(
    searchParams.get("drafts") === "true",
  );
  const [viewedFilter, setViewedFilter] = useState(
    searchParams.get("viewed") ?? "",
  );

  // Sync state with URL params when they change externally
  useEffect(() => {
    setSearchQuery(searchParams.get("search") ?? "");
    setSelectedTag(searchParams.get("tag") ?? "");
    setAdminFilter(searchParams.get("admin") ?? "");
    setDraftsMode(searchParams.get("drafts") === "true");
    setViewedFilter(searchParams.get("viewed") ?? "");
  }, [searchParams]);

  // Handle search submission
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();

    const params = new URLSearchParams(searchParams);
    if (searchQuery.trim()) {
      params.set("search", searchQuery.trim());
    } else {
      params.delete("search");
    }

    // Reset to first page when searching
    params.delete("page");

    router.push(`lore?${params.toString()}`);
  };

  // Handle tag selection
  const handleTagClick = (tag: string) => {
    const isSelected = selectedTag === tag;
    const params = new URLSearchParams(searchParams);

    if (isSelected) {
      params.delete("tag");
      setSelectedTag("");
    } else {
      params.set("tag", tag);
      setSelectedTag(tag);
    }

    // Reset to first page when changing filters
    params.delete("page");

    router.push(`lore?${params.toString()}`);
  };

  // Handle author filter toggle
  const handleAuthorToggle = (value: string) => {
    const params = new URLSearchParams(searchParams);
    const isCurrentlySelected = adminFilter === value;

    if (isCurrentlySelected) {
      params.delete("admin");
      setAdminFilter("");
    } else {
      params.set("admin", value);
      setAdminFilter(value);
    }

    params.delete("page");
    router.push(`lore?${params.toString()}`);
  };

  // Handle viewed filter toggle
  const handleViewedToggle = (value: string) => {
    const params = new URLSearchParams(searchParams);
    const isCurrentlySelected = viewedFilter === value;

    if (isCurrentlySelected) {
      params.delete("viewed");
      setViewedFilter("");
    } else {
      params.set("viewed", value);
      setViewedFilter(value);
    }

    params.delete("page");
    router.push(`lore?${params.toString()}`);
  };

  // Handle drafts mode toggle
  const handleDraftsModeToggle = () => {
    const params = new URLSearchParams();

    if (!draftsMode) {
      params.set("drafts", "true");
      setDraftsMode(true);
      // Clear other filters when entering drafts mode
      setSearchQuery("");
      setSelectedTag("");
      setAdminFilter("");
      setViewedFilter("");
    } else {
      setDraftsMode(false);
    }

    router.push(`lore?${params.toString()}`);
  };

  // Clear all filters
  const clearFilters = () => {
    setSearchQuery("");
    setSelectedTag("");
    setAdminFilter("");
    setDraftsMode(false);
    setViewedFilter("");
    router.push("lore");
  };

  // Check if any filters are active (including from URL params)
  const hasActiveFilters =
    searchParams.get("search") ||
    searchParams.get("tag") ||
    searchParams.get("admin") ||
    searchParams.get("drafts") === "true" ||
    searchParams.get("viewed") ||
    searchParams.get("updated") === "true";

  return (
    <div className="bg-card rounded-lg p-5 border shadow-sm">
      {/* Search Bar */}
      <form onSubmit={handleSearch} className="mb-5">
        <div className="flex gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              type="search"
              placeholder="Поиск по записям..."
              className="pl-9 h-10"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              disabled={draftsMode}
            />
          </div>
          <Button
            type="submit"
            size="default"
            className="px-5"
            disabled={draftsMode}
          >
            Найти
          </Button>
        </div>
      </form>

      {/* Clear Filters Button */}
      {hasActiveFilters && (
        <div className="mb-4">
          <Button
            variant="outline"
            size="sm"
            onClick={clearFilters}
            className="h-8 w-full text-xs border-destructive/30 text-destructive hover:bg-destructive hover:text-destructive-foreground bg-transparent"
          >
            <X className="h-3.5 w-3.5 mr-1.5" />
            Сбросить все фильтры
          </Button>
        </div>
      )}

      {/* Filters Grid */}
      <div className="space-y-5">
        {/* Drafts Mode */}
        <div className="pb-4 border-b border-input">
          <Toggle
            pressed={draftsMode}
            onPressedChange={handleDraftsModeToggle}
            className="w-full justify-center font-medium"
            variant="outline"
          >
            <span className="text-sm">Черновики</span>
          </Toggle>
          <div className="w-full justify-center pt-2">
            <CustomLink href={"./lore/search"}>
              <Button className="w-full">Поиск по содержимому</Button>
            </CustomLink>
          </div>
        </div>

        {/* Main Filters - Disabled when in drafts mode */}
        <div
          className={
            draftsMode
              ? "opacity-40 pointer-events-none space-y-5"
              : "space-y-5"
          }
        >
          {/* Tags Section */}
          <div>
            <h3 className="text-sm font-semibold text-foreground mb-3 uppercase tracking-wide">
              Теги
            </h3>
            <div className="flex flex-wrap gap-2">
              {tags.map((tag) => (
                <Badge
                  key={tag.name}
                  variant={selectedTag === tag.name ? "default" : "secondary"}
                  className={`cursor-pointer transition-all hover:scale-105 px-3 py-1 text-xs ${
                    selectedTag === tag.name
                      ? "shadow-md ring-2 ring-primary/20"
                      : "hover:bg-secondary/80"
                  }`}
                  onClick={() => handleTagClick(tag.name)}
                >
                  {tag.name}
                </Badge>
              ))}
            </div>
          </div>

          {/* Author Filter */}
          <div>
            <h3 className="text-sm font-semibold text-foreground mb-3 uppercase tracking-wide">
              Автор
            </h3>
            <div className="flex gap-2">
              <Toggle
                pressed={adminFilter === "admin"}
                onPressedChange={() => handleAuthorToggle("admin")}
                className="flex-1 text-sm"
                variant="outline"
              >
                DM
              </Toggle>
              <Toggle
                pressed={adminFilter === "non-admin"}
                onPressedChange={() => handleAuthorToggle("non-admin")}
                className="flex-1 text-sm"
                variant="outline"
              >
                Игроки
              </Toggle>
            </div>
          </div>

          {/* View Status Filter */}
          <div>
            <h3 className="text-sm font-semibold text-foreground mb-3 uppercase tracking-wide">
              Статус просмотра
            </h3>
            <div className="flex gap-2 mb-2">
              <Toggle
                pressed={viewedFilter === "viewed"}
                onPressedChange={() => handleViewedToggle("viewed")}
                className="flex-1 text-sm"
                variant="outline"
              >
                Просмотренные
              </Toggle>
              <Toggle
                pressed={viewedFilter === "unviewed"}
                onPressedChange={() => handleViewedToggle("unviewed")}
                className="flex-1 text-sm"
                variant="outline"
              >
                Непросмотренные
              </Toggle>
            </div>
            <Toggle
              pressed={viewedFilter === "updated"}
              onPressedChange={() => handleViewedToggle("updated")}
              className="w-full justify-center text-sm"
              variant="outline"
            >
              Обновленные записи
            </Toggle>
          </div>
        </div>
      </div>

      {/* Create Button */}
      <div className="mt-5 pt-5 border-t border-input">
        <CustomLink href={`lore/create`}>
          <Button
            variant="default"
            size="default"
            className="w-full font-medium shadow-sm"
          >
            <Plus className="w-4 h-4 mr-2" />
            Создать запись
          </Button>
        </CustomLink>
      </div>
    </div>
  );
}
