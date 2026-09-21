import { NextRequest, NextResponse } from "next/server"
import { getQuestionById } from "@/server/questions/question" // Путь к вашей серверной функции

// Пример пути: /api/questions/123?answerId=456&roomName=my-room
export async function GET(
  req: NextRequest,
  { params }: { params: { questionId: string } }
) {
  try {
    const { searchParams } = new URL(req.url)
    
    const {questionId}  = await params
    const answerId = searchParams.get("answerId") // может быть null
    const roomName = searchParams.get("roomName")

    if (!roomName) {
      return NextResponse.json(
        { error: "roomName is required" },
        { status: 400 }
      )
    }

    // Вызываем вашу серверную функцию
    // Приводим answerId к нужному типу (number | null), если нужно
    const data = await getQuestionById(
      roomName,
      questionId,
      answerId || null
    )

    return NextResponse.json(data)
  } catch (e) {
    console.error("API ERROR:", e)
    return NextResponse.json(
      { error: "Failed to fetch question" },
      { status: 500 }
    )
  }
}