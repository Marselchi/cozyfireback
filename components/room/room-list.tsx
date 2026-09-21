"use client"

import RoomCard from "@/components/room/room-card"
import CreateRoomButton from "@/components/room/create-room-button"
import { createRoomAction } from "@/server/room/actions"

interface Room {
  id: string
  name: string
  display_name: string
  description: string
  player_count: number
  author: string
}

interface RoomListProps {
  rooms: Room[]
}

export default function RoomList({ rooms }: Readonly<RoomListProps>) {

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
      {rooms.map((room) => (
        <RoomCard key={room.id} room={room} />
      ))}
      <CreateRoomButton createRoomAction={createRoomAction} />
    </div>
  )
}
