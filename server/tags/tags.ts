"use server";
import "server-only";
import { getWithAuth } from "@/lib/auth/apiClient";

export async function getAllTags(roomName: string) {
  const [error, data] = await getWithAuth(`/tags/${roomName}/all`);
  if (error) {
    console.error(error);
    return [];
  }
  return data;
}
