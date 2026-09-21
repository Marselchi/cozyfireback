"use client";

import Link from "next/link";
import { ChevronDown, Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { NavRoom } from "@/types/room";
import CustomLink from "../no-prefetch-link";

interface RoomSelectorProps {
  currentRoom: NavRoom;
  allRooms: NavRoom[];
}

export function RoomSelector({
  currentRoom,
  allRooms,
}: Readonly<RoomSelectorProps>) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          className="flex items-center gap-2 px-3 text-base font-semibold hover:bg-accent"
        >
          <span className="max-w-50 truncate">{currentRoom.name}</span>
          <ChevronDown className="h-4 w-4 opacity-50" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="w-56">
        {allRooms.map((room) => (
          <DropdownMenuItem key={room.id} asChild>
            <CustomLink
              href={`/rooms/${room.id}-${room.slug}`}
              className="flex items-center justify-between"
            >
              <span className="truncate">{room.name}</span>
              {room.id === currentRoom.id && <Check className="h-4 w-4" />}
            </CustomLink>
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
