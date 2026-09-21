"use client"

import { useActionState, useState } from "react"
import { Button } from "@/components/ui/button"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { LogOut, Settings } from "lucide-react"
import { useRouter } from "next/navigation"
import { useFormStatus } from "react-dom"
import { Input } from "../ui/input"
import { enterCode } from "@/server/room/room"
import { logOut } from "@/server/user/user"

interface ProfileSectionProps {
  user: {
    email: string
    nickname: string
  }
}


interface Error {
  error?: string
  success?: boolean
}

const initialState: Error = {
  error: "",
  success: false,
}

export default function ProfileSection({ user }: Readonly<ProfileSectionProps>) {
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [error, action] = useActionState(enterCode, initialState)
  const router = useRouter()
  const handleLogout = async () => {
    try {
      setIsLoggingOut(true)
      await logOut()
      router.push("/login")
    } catch (error) {
      console.error("Ошибка выхода:", error)
    } finally {
      setIsLoggingOut(false)
    }
  }

  return (
    <div className="bg-card rounded-lg p-6 shadow-xs border">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Avatar className="h-12 w-12">
            <AvatarImage src={""} alt={user.nickname ?? user.email} />
            <AvatarFallback>
              {user.nickname ? user.nickname.substring(0, 2).toUpperCase() : user.email.substring(0, 2).toUpperCase()}
            </AvatarFallback>
          </Avatar>
          <div>
            <h2 className="font-medium">{user.nickname}</h2>
            <p className="text-sm text-muted-foreground">{user.email}</p>
          </div>
          <form className="flex items-center space-x-2 ml-auto">
            <Input
              id="code"
              name="code"
              type="text"
              placeholder="Введите код комнаты"
              maxLength={8}
              required
            />
            <SubmitButton action={action} />
            {error?.error && <p className="text-red-500">{error.error}</p>}
          </form>
        </div>
        <div className="flex space-x-2">
          {/* <Button variant="outline" size="icon">
            <Settings className="h-4 w-4" />
            <span className="sr-only">Настройки</span>
          </Button> */}
          <Button variant="destructive" size="icon" onClick={handleLogout} disabled={isLoggingOut}>
            <LogOut className="h-4 w-4" />
            <span className="sr-only">Выйти</span>
          </Button>
        </div>
      </div>
    </div>
  )
}


function SubmitButton({action} : Readonly<{ action: (payload: FormData) => void }>) {
  const { pending } = useFormStatus()
  return (
    <Button 
      type="submit" 
      size="sm"
      disabled={pending}
      className="h-9 px-4"
      formAction={action}
    >
      {pending ? 'Подтверждение...' : 'Применить'}
    </Button>
  )
}