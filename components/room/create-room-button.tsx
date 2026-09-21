"use client"

import { useActionState, useEffect, useState } from "react"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Plus } from "lucide-react"
import { useRouter } from "next/navigation"

interface Error {
  error?: string
  success: boolean
}

const initialState: Error = {
  error: "",
  success: false,
}

interface CreateRoomButtonProps {
  createRoomAction: (previousState: Error, formData: FormData) => Promise<Error>
}

export default function CreateRoomButton({ createRoomAction }: Readonly<CreateRoomButtonProps>) {
  const router = useRouter()
  const [isOpen, setIsOpen] = useState(false)
  const [error, action, isPending] = useActionState(createRoomAction, initialState)
  useEffect(() => {
    if (error.success) {
      // Закрываем диалог
      setIsOpen(false);
      // Принудительно обновляем данные на странице
      router.refresh();
    }
  }, [error.success]);
  return (
    <Dialog open={isOpen} onOpenChange={setIsOpen}>
      <DialogTrigger asChild>
        <Card className="h-full flex items-center justify-center p-6 border-dashed cursor-pointer hover:bg-accent transition-colors">
          <div className="flex flex-col items-center text-center">
            <div className="rounded-full bg-primary/10 p-3 mb-3">
              <Plus className="h-6 w-6 text-primary" />
            </div>
            <h3 className="font-medium">Создать новую комнату</h3>
            <p className="text-sm text-muted-foreground mt-1">Начать новое приключение</p>
          </div>
        </Card>
      </DialogTrigger>
      <DialogContent className="sm:max-w-[425px]">
        <form action={action}>
          <DialogHeader>
            <DialogTitle>Создать новую комнату</DialogTitle>
            <DialogDescription>Заполните поля</DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="name">URL комнаты</Label>
              <Input
                id="name"
                name="name"
                placeholder="sluged-url"
                required
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="display_name">Отображаемое имя</Label>
              <Input
                id="display_name"
                name="display_name"
                placeholder="Что угодно"
                required
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="description">Описание (опционально)</Label>
              <Textarea
                id="description"
                name="description"
                placeholder="В некотором царстве..."
              />
            </div>
          </div>
          <DialogFooter>
            {error.error && <p className="text-red-500">{error.error}</p>}
            <Button type="button" variant="outline" onClick={() => setIsOpen(false)}>
              Отмена
            </Button>
            <Button type="submit" disabled={isPending}>
              {isPending ? "Создаю..." : "Создать комнату"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}