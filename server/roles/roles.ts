'use server'
import 'server-only'
import { getWithAuth } from '@/lib/auth/apiClient'

export interface Role {
  id: string 
  name: string
}



export async function getAllRoles(roomName : string) {
    const [error, data] = await getWithAuth(`/roles/${roomName}/all`)
    if (error) {
      console.error(error)
      return []
    }


    return data

}


export async function getAllRolesEdit(roomName : string) {
    const [error, data] = await getWithAuth(`/roles/${roomName}/allEdit`)
    if (error) {
      console.error(error)
      return []
    }


    return data

}
