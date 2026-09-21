"use server";
import "server-only";
import { cacheLife, revalidatePath } from "next/cache";
import { CampaignData } from "@/types/text";
import { getAnon, getWithAuth, sendWithAuth } from "@/lib/auth/apiClient";
import { NavRoom, Room } from "@/types/room";
import { RoomContextResponse } from "@/types/springTypes";

interface Error {
  error?: string;
  success?: boolean;
}

const _fetchRoomIdByName = async (name: string): Promise<string> => {
  "use cache";
  cacheLife("weeks");

  const [error, data] = await getAnon(`/rooms/${name}`);
  if (error || !data) {
    console.error(error);
    return "";
  }

  return data.toString();
};

//Отдельная функция для читаемости
export const getRoomId = _fetchRoomIdByName;

//CHISTO
export const getCampaignData = async (name: string): Promise<CampaignData> => {
  const [error, data] = await getWithAuth(`/rooms/${name}/details`);
  if (error || !data) {
    console.error(error);
    throw error;
  }

  const formattedData: CampaignData = {
    currentDate: data.currentDate,
    currentSituation: data.currentSituation,
    lastSession: data.lastSession,
  };
  return formattedData;
};

//chisto
export const getRoomData = async (name: string): Promise<Room> => {
  const [error, data] = await getWithAuth(`/rooms/${name}/details`);
  if (error || !data) {
    console.error(error);
    throw error;
  }

  const formattedData: Room = {
    id: name ?? "0",
    url: name,
    displayName: data.name,
  };
  return formattedData;
};

export const getRoomContext = async (id: string): Promise<NavRoom> => {
  const [error, data] = await getWithAuth(`/rooms/${id}/context`);
  if (error) {
    console.error(error);
    throw error;
  }

  const formattedData: NavRoom = {
    id: id ?? "0",
    slug: data.url,
    name: data.name,
    description: data.description,
    isAdmin: data.currentUserCreator,
    questionCount: data.questionCount,
  };
  return formattedData;
};

export const getRoomDataId = async (roomId: string): Promise<Room> => {
  const [error, data] = await getWithAuth(`/rooms/${roomId}/details`);
  if (error) {
    console.error(error);
    throw error;
  }

  const formattedData: Room = {
    id: roomId ?? "0",
    url: data.url,
    displayName: data.name,
  };
  return formattedData;
};

export const createRoom = async (roomData: {
  name: string;
  display_name: string;
  description?: string;
}) => {
  const [error] = await sendWithAuth(
    "/rooms",
    "POST",
    {
      name: roomData.display_name,
      url: roomData.name,
      description: roomData.description,
    },
    { expectsJson: false },
  );
  if (error) {
    console.error(error);
  }
};

export async function enterCode(previousState: Error, formData: FormData) {
  const [error] = await sendWithAuth(
    "/invitations/use",
    "POST",
    { code: formData.get("code") as string },
    { expectsJson: false },
  );
  if (error) {
    return { error: error.message };
  }
  revalidatePath("/");
  return {
    success: true,
  };
}

export const getAllRoomsNav = async (): Promise<NavRoom[]> => {
  const [error, data] = await getWithAuth<RoomContextResponse[]>("/rooms/my");
  if (error || !data) {
    console.error(error);
    return [];
  }
  const rooms: NavRoom[] = data.map((room) => ({
    id: room.id.toString(),
    slug: room.url,
    name: room.name,
    description: room.description,
    isAdmin: room.isCurrentUserCreator,
  }));

  return rooms;
};
