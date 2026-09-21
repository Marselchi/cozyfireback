"use server"

import { z } from 'zod'
import { redirect } from 'next/navigation'
import { signUpUser } from '@/server/auth/actions'

const authSchema = z.object({
  email: z.string().email('Неверный формат email'),
  password: z.string().min(6, 'Пароль должен быть не короче 6 символов'),
  code: z.string().length(8),
  name: z.string().min(5, 'Ник должен быть минимум 5 символов').max(15, 'Ник должен быть максимум 15 символов')
})

type AuthOutput = {
  errors?: {
    email?: string[]
    name?: string[]
    password?: string[]
    code?: string[]
  }
  formError?: string
  success?: boolean
}

export async function signup(
  prevState: AuthOutput,
  formData: FormData
): Promise<AuthOutput> {

  const parseResult = authSchema.safeParse({
    email: formData.get("email"),
    password: formData.get("password"),
    code: formData.get("code"),
    name: formData.get("name"),
  });

  if (!parseResult.success) {
    return {
      errors: parseResult.error.flatten().fieldErrors,
    };
  }

  await signUpUser({username: parseResult.data.name, email: parseResult.data.email, password: parseResult.data.password, code: parseResult.data.code})


  redirect("/login");
}