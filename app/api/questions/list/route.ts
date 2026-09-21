import { NextRequest, NextResponse } from "next/server"
import { getFilterQuestions } from "@/server/questions/question"

export async function POST(req: NextRequest) {
  try {
    const body = await req.json()

    const data = await getFilterQuestions(body)

    return NextResponse.json(data)
  } catch (e) {
    console.error("API ERROR:", e)
    return NextResponse.json(
      { content: [], page: undefined },
      { status: 500 }
    )
  }
}