"use server";
import "server-only";
import { sendWithAuth } from "@/lib/auth/apiClient";
import { revalidatePath } from "next/cache";

//TODO: maybe make optimistic ui idk too lazy

export async function removeUserRole(formData: FormData) {
  const userId = Number(formData.get("userId"));
  const roleId = Number(formData.get("roleId"));
  const roomName = String(formData.get("roomName"));

  const [error] = await sendWithAuth(
    `/accounts/${roomName}/${userId}/role`,
    "DELETE",
    roleId,
    { expectsJson: false },
  );
  if (!error) {
    revalidatePath(`/rooms/${roomName}/admin`);
  }
}

export async function assignUserRole(formData: FormData) {
  const userId = Number(formData.get("userId"));
  const roleId = Number(formData.get("roleId"));
  const roomName = String(formData.get("roomName"));

  const [error] = await sendWithAuth(
    `/accounts/${roomName}/${userId}/role`,
    "POST",
    roleId,
    { expectsJson: false },
  );
  if (!error) {
    revalidatePath(`/rooms/${roomName}/admin`);
  }
}

export async function replaceUserRole(formData: FormData) {
  const userId = Number(formData.get("userId"));
  const oldRoleId = Number(formData.get("oldRoleId"));
  const newRoleId = Number(formData.get("newRoleId"));
  const roomName = String(formData.get("roomName"));

  const [error] = await sendWithAuth(
    `/accounts/${roomName}/${userId}/role`,
    "PUT",
    { oldRoleId, newRoleId },
    { expectsJson: false },
  );
  if (!error) {
    revalidatePath(`/rooms/${roomName}/admin`);
  }
}
