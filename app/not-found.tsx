"use client"

import { useRouter } from "next/navigation"



export default function NotFound() {
  const router = useRouter()
  const handleRedirect = () => {
    router.back()
  }
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-gray-950 text-gray-100">
      <h1 className="text-4xl font-bold">404</h1>
      <h2 className="mb-4 text-xl">Страница не найдена</h2>
      <p className="mb-8 text-center">Я не знаю куда ты лезешь, но этого нет</p>
      <p className="mb-8 text-center text-sm">(или у тебя нет на это прав)</p>
      <button
        onClick={handleRedirect}
        className="rounded-md bg-muted px-4 py-2 hover:bg-muted/90 mb-8"
      >
        Вернуться обратно
      </button>
      <a href="/" className="rounded-md bg-muted px-2 py-1 hover:bg-muted/90">
        На главную
      </a>
    </div>
  )
}

