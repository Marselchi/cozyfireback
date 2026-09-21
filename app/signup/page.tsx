"use client"

import { signup } from './actions'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Loader2 } from 'lucide-react'
import { redirect } from 'next/navigation'
import { useActionState } from 'react'


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

const initialState: AuthOutput = {
  errors: {},
  formError: undefined,
  success: false
}

export default function LoginPage() {
  const [state, action, pending] = useActionState(signup, initialState)

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-950 p-4">
      <Card className="w-full max-w-md border-gray-800 bg-gray-900 text-gray-100">
        <CardHeader className="space-y-1 pb-0">
          <CardTitle className="text-2xl font-bold">Зарегистрироваться</CardTitle>
          <CardDescription className="text-gray-400">
            Нет кода? Тогда вам сюда не надо
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form 
            action={action} 
            className="space-y-4"
          >
            <div className="space-y-1">
              <Label htmlFor="email">Почта</Label>
              <Input
                id="email"
                name="email"
                type="email"
                placeholder="Введите почту..."
                required
                className="border-gray-700 bg-gray-800 text-gray-100"
              />
              {state.errors?.email && (
                <Alert variant="destructive">
                  <AlertDescription>{state.errors.email}</AlertDescription>
                </Alert>
              )}
            </div>
            <div className="space-y-1">
              <Label htmlFor="email">Никнейм</Label>
              <Input
                id="name"
                name="name"
                type="text"
                placeholder="Bobik2007..."
                required
                className="border-gray-700 bg-gray-800 text-gray-100"
              />
              {state.errors?.name && (
                <Alert variant="destructive">
                  <AlertDescription>{state.errors.name}</AlertDescription>
                </Alert>
              )}
            </div>
            <div className="space-y-1">
              <Label htmlFor="password">Пароль</Label>
              <Input
                id="password"
                name="password"
                type="password"
                required
                className="border-gray-700 bg-gray-800 text-gray-100"
              />
              {state.errors?.password && (
                <Alert variant="destructive">
                  <AlertDescription>{state.errors.password}</AlertDescription>
                </Alert>
              )}
            </div>
            <div className="space-y-1">
              <Label htmlFor="email">КОД</Label>
              <Input
                id="code"
                name="code"
                type="text"
                placeholder="XXXXXXXX"
                required
                className="border-gray-700 bg-gray-800 text-gray-100"
              />
              {state.errors?.code && (
                <Alert variant="destructive">
                  <AlertDescription>{state.errors.code}</AlertDescription>
                </Alert>
              )}
            </div>
            <Button 
              type="submit" 
              className="w-full bg-primary hover:bg-primary/90" 
              disabled={pending}
            >
              {pending ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Проверяю
                </>
              ) : (
                "Зарегистрироваться"
              )}
            </Button>
          </form>
        </CardContent>
        <CardFooter className="flex justify-center gap-1 text-sm text-gray-400">
          <span>Уже есть аккаунт?</span>
          <Button
            variant="link"
            className="p-0 text-primary hover:text-primary/90"
            onClick={() => redirect("/login")}
          >
            Войти
          </Button>
        </CardFooter>
      </Card>
    </div>
  )
}