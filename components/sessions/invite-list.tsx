"use client"

import { X } from "lucide-react"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuCheckboxItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Badge } from "@/components/ui/badge"
import type { IdName } from "@/types/springTypes"

interface InviteListProps {
  users: IdName[]
  selectedIds: number[]
  onSelectionChange: (ids: number[]) => void
}

export function InviteList({ users, selectedIds, onSelectionChange }: Readonly<InviteListProps>) {
  const toggle = (userId: number) => {
    if (selectedIds.includes(userId)) {
      onSelectionChange(selectedIds.filter((id) => id !== userId))
    } else {
      onSelectionChange([...selectedIds, userId])
    }
  }

  const selectedUsers = users.filter((u) => selectedIds.includes(u.id))

  return (
    <div className="space-y-3">
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="outline"
            className="w-full justify-between border-border bg-additional hover:bg-secondary"
          >
            <span className="text-muted-foreground">
              {selectedIds.length === 0
                ? "Выберите игроков для приглашения..."
                : `Выбрано: ${selectedIds.length}`}
            </span>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent className="w-64 bg-popover border-border">
          {users.length === 0 && (
            <div className="px-3 py-2 text-sm text-muted-foreground">Нет участников</div>
          )}
          {users.map((user) => (
            <DropdownMenuCheckboxItem
              key={user.id}
              checked={selectedIds.includes(user.id)}
              onCheckedChange={() => toggle(user.id)}
              className="flex gap-3 py-2"
            >
              <Avatar className="h-7 w-7">
                <AvatarFallback className="bg-muted text-muted-foreground text-xs">
                  {user.name.slice(0, 2).toUpperCase()}
                </AvatarFallback>
              </Avatar>
              <span className="text-sm font-medium text-foreground">{user.name}</span>
            </DropdownMenuCheckboxItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>

      {selectedUsers.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {selectedUsers.map((user) => (
            <Badge
              key={user.id}
              variant="secondary"
              className="flex items-center gap-1.5 py-1 px-2 bg-muted border-input border"
            >
              <Avatar className="h-4 w-4">
                <AvatarFallback className="text-[10px]">
                  {user.name.slice(0, 2).toUpperCase()}
                </AvatarFallback>
              </Avatar>
              <span className="text-xs">{user.name}</span>
              <button
                onClick={() => toggle(user.id)}
                className="ml-0.5 hover:text-destructive transition-colors"
              >
                <X className="h-3 w-3" />
              </button>
            </Badge>
          ))}
        </div>
      )}
    </div>
  )
}
