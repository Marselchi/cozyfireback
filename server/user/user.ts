"use server";
import "server-only";
import { redirect } from "next/navigation";
import { revalidatePath } from "next/cache";
import { withAuth } from "@/lib/auth/withAuth";
import { getWithAuth, sendWithAuth } from "@/lib/auth/apiClient";
import { RoomAccountResponse } from "@/types/manage";
import { IdName } from "@/types/springTypes";
import { cookies } from "next/headers";
import {
  ACCESS_TOKEN_COOKIE_NAME,
  REFRESH_TOKEN_COOKIE_NAME,
} from "@/lib/auth/authUtils";

export interface User {
  id: string; //Опасно
  email: string;
  nickname: string;
}

export interface RoomUser {
  id: string;
  name: string;
  roles: string[];
  roomId: string;
  isAdmin: boolean;
}

export async function logOut(): Promise<void> {
  await sendWithAuth("/auth/logout", "POST", null, { expectsJson: false });
  const cookieStore = await cookies();
  cookieStore.delete(ACCESS_TOKEN_COOKIE_NAME);
  cookieStore.delete(REFRESH_TOKEN_COOKIE_NAME);
  redirect("/login");
}

export const getMainUser = async (): Promise<User> => {
  const [error, data] = await getWithAuth("/auth/me");
  if (error) {
    console.error(error);
    throw error;
  }
  return {
    email: data.email ?? "",
    id: data.id,
    nickname: data.username ?? "",
  };
};

export async function getRoomUser(roomName: string): Promise<RoomUser> {
  const [error, data] = await getWithAuth(`/accounts/${roomName}`);
  if (error) {
    console.error(error);
    throw error;
  }
  return {
    id: data.id,
    name: data.username,
    roles: data.roles.map((role: { id: number; name: string }) =>
      String(role.id),
    ),
    roomId: roomName,
    isAdmin: data.admin,
  };
}

export async function getAllUsersInRoom(
  roomName: string,
): Promise<RoomAccountResponse[]> {
  const [error, data] = await getWithAuth<RoomAccountResponse[]>(
    `/accounts/${roomName}/all`,
  );
  if (error || !data) {
    console.error(error);
    return [];
  }

  return data.map(
    (item): RoomAccountResponse => ({
      id: item.id,
      profileName: item.profileName,
      username: item.username,
      roles: item.roles ?? [],
    }),
  );
}

export async function getAllUsersInRoomIdName(
  roomName: string,
): Promise<IdName[]> {
  const [error, data] = await getWithAuth<RoomAccountResponse[]>(
    `/accounts/${roomName}/all`,
  );
  if (error || !data) {
    console.error(error);
    return [];
  }

  return data.map(
    (item): IdName => ({
      id: item.id,
      name: item.username,
    }),
  );
}

export async function updateUserName(roomName: string, newName: string) {
  const [error] = await sendWithAuth(
    `/accounts/${roomName}/update`,
    "PUT",
    {
      name: newName,
    },
    { expectsJson: false },
  );
  if (error) {
    console.error("Error updating user name:", error);
    return { success: false, message: "Ошибка обновления имени" };
  }
  revalidatePath(`/rooms/${roomName}`);
  return { success: true, message: "Имя успешно обновлено" };
}
