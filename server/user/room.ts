'use server'
import 'server-only'
import { getWithAuth } from '@/lib/auth/apiClient'
import { RoomContextResponse } from '@/types/springTypes'

interface Room {
  id: string
  name: string
  display_name: string
  description: string
  player_count: number
  author: string
}

export const getUserRooms = async (): Promise<Room[]> => {
    const [error, data] = await getWithAuth<RoomContextResponse[]>("/rooms/my")
    if (error|| !data) {
      console.error(error)
      return []
    }

    const roomsWithPlayerCount: Room[] = data.map((room
    ) => ({
        id: room.id.toString(),
        name: room.url,
        display_name: room.name,
        description: room.description,
        player_count: room.memberCount,
        author: room.creator.username,
    }));

    return roomsWithPlayerCount;
}
