'use server'
import 'server-only'
import { ItemType } from "@/types/manage"
import { revalidatePath } from 'next/cache'
import { sendWithAuth } from '@/lib/auth/apiClient'


export async function deleteItem(type: ItemType, formData: FormData) {
  const id = formData.get("id") as string
  const roomName = formData.get("roomName") as string
  const [error, data] = await sendWithAuth(`/${type}s/${roomName}/${id}`, "DELETE", null, {expectsJson: false})
  revalidatePath(`/rooms/[roomName]/admin/${type}s`, 'page') 
  return {
      error: "",
      success: true,
    }
}

export async function updateItem(type: ItemType, previousState: Error, formData: FormData) {
  const name = formData.get("name") as string
  const id = formData.get("id") as string
  const roomName = formData.get("roomName") as string
  const [error, data] = await sendWithAuth(`/${type}s/${roomName}/${id}`, "PUT", {
    name: name
  }, {expectsJson: false})
  revalidatePath(`/rooms/[roomName]/admin/${type}s`, 'page')
  return {
      error: "",
      success: true,
    }
}

export interface Error {
  error: string
  success: boolean
}

export async function createItem(type: ItemType, previousState: Error, formData: FormData ) {
  const name = formData.get("name") as string
  const roomName = formData.get("roomName") as string
  if (!name || name.trim().length === 0) {
    return { error: "Необходимо имя", success: false }
  }

  try {
      const [error, data] = await sendWithAuth(`/${type}s/${roomName}`, "POST", {
        name: name
      }, {expectsJson: false})

      if (error){
        throw new Error(`${type} create error`)
      }
    revalidatePath(`/rooms/[roomName]/admin/${type}s`, 'page')
    return { success: true, error: "" }
  } catch (error) {
    return { error: "Ошибка создания", success: false }
  }
}

