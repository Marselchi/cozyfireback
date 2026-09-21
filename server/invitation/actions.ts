"use server"
import "server-only"
import { createInvitation, deleteInvitation, getAllInvitations } from "./invitation"
import { getAllUsersInRoom } from "../user/user"

export async function kickUser(userId: string) {
//todo: Добавить кик
}

export async function deleteAuthCode(codeId: string) {
  await deleteInvitation(codeId)
}



