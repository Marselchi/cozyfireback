"use server";
import "server-only";
import { revalidatePath } from "next/cache";
import { getWithAuth, sendWithAuth } from "@/lib/auth/apiClient";

export async function createInvitation(
  prevState: { error?: string; codes: AuthCode[] } | null,
  formData: FormData,
  roomName: string,
): Promise<{ error?: string; codes: AuthCode[] }> {
  const [error, response] = await sendWithAuth(
    `/invitations/${roomName}`,
    "POST",
  );

  if (error) {
    console.error(error);
    return { codes: [], error: "Не удалось создать код" };
  }

  // Добавляем новый код к существующему списку (если он был)
  const updatedCodes = [...(prevState?.codes || []), response];

  revalidatePath("/admin");
  return { codes: updatedCodes };
}

interface AuthCode {
  id: string;
  code: string;
}

export async function getAllInvitations(roomName: string) {
  const [error, data] = await getWithAuth(`/invitations/${roomName}/all`);

  if (error) {
    console.error(error);
    return [];
  }

  const formattedData: AuthCode[] = data.map((item: AuthCode) => ({
    id: item.id,
    code: item.code,
  }));

  return formattedData;
}

export async function deleteInvitation(id: string) {
  const [error] = await sendWithAuth(`/invtitations/${id}`, "DELETE", null, {
    expectsJson: false,
  });
  if (!error) {
    revalidatePath("/rooms/[roomName]/admin");
  }
}
