'use server'

import { revalidatePath } from 'next/cache'
import { z } from 'zod'
import { redirect } from 'next/navigation'
import { loginUser } from '../../server/auth/actions'

const authSchema = z.object({
  username: z.string(),
  password: z.string().min(6, 'Пароль должен быть не короче 6 символов')
})

type AuthOutput = {
  errors?: {
    username?: string[]
    password?: string[]
  }
  formError?: string
  success?: boolean
}

export async function login(prevState: AuthOutput, formData: FormData): Promise<AuthOutput> {

  // 1. Валидация данных через Zod
  const parseResult = authSchema.safeParse({
    username: formData.get('username'),
    password: formData.get('password')
  })

  if (!parseResult.success) {
    return {
      errors: parseResult.error.flatten().fieldErrors
    }
  }

  // 2. Попытка аутентификации
  const data = await loginUser({username: parseResult.data.username, password: parseResult.data.password})

  // if (error) {
  //   return {
  //     formError: 'Неверный email или пароль'
  //   }
  // }

  revalidatePath('/', 'layout')
  redirect('/')
}

