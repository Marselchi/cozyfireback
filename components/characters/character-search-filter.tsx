"use client"

import type React from "react"

import { useState, useEffect } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Plus, Search, X } from "lucide-react"
import { Toggle } from "@/components/ui/toggle"
import CustomLink from "../no-prefetch-link"

export default function CharacterSearchFilter() {
  const router = useRouter()
  const searchParams = useSearchParams()

  const [searchQuery, setSearchQuery] = useState(searchParams.get("search") ?? "")
  const [adminFilter, setAdminFilter] = useState(searchParams.get("admin") ?? "")

  // Sync state with URL params when they change externally
  useEffect(() => {
    setSearchQuery(searchParams.get("search") ?? "")
    setAdminFilter(searchParams.get("admin") ?? "")
  }, [searchParams])

  // Handle search submission
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()

    const params = new URLSearchParams(searchParams)
    if (searchQuery.trim()) {
      params.set("search", searchQuery.trim())
    } else {
      params.delete("search")
    }

    // Reset to first page when searching
    params.delete("page")

    router.push(`characters?${params.toString()}`)
  }

  // Handle author filter toggle
  const handleAuthorToggle = (value: string) => {
    const params = new URLSearchParams(searchParams)
    const isCurrentlySelected = adminFilter === value

    if (isCurrentlySelected) {
      params.delete("admin")
      setAdminFilter("")
    } else {
      params.set("admin", value)
      setAdminFilter(value)
    }

    params.delete("page")
    router.push(`characters?${params.toString()}`)
  }

  // Clear all filters
  const clearFilters = () => {
    setSearchQuery("")
    setAdminFilter("")
    router.push("characters")
  }

  // Check if any filters are active
  const hasActiveFilters =
    searchParams.get("search") ||
    searchParams.get("admin")

  return (
    <div className="bg-card rounded-lg p-5 border shadow-sm">
      {/* Search Bar */}
      <form onSubmit={handleSearch} className="mb-5">
        <div className="flex gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              type="search"
              placeholder="Поиск по персонажам..."
              className="pl-9 h-10"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <Button type="submit" size="default" className="px-5">
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
            className="h-8 text-xs border-destructive/30 text-destructive hover:bg-destructive hover:text-destructive-foreground bg-transparent"
          >
            <X className="h-3.5 w-3.5 mr-1.5" />
            Сбросить все фильтры
          </Button>
        </div>
      )}

      {/* Filters */}
      <div className="space-y-5">
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
      </div>

      {/* Create Button */}
      <div className="mt-5 pt-5 border-t">
        <CustomLink href={`characters/create`}>
          <Button variant="default" size="default" className="w-full font-medium shadow-sm">
            <Plus className="w-4 h-4 mr-2" />
            Создать персонажа
          </Button>
        </CustomLink>
      </div>
    </div>
  )
}
