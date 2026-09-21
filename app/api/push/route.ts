import { sendWithAuth } from "@/lib/auth/apiClient";
import { NextRequest, NextResponse } from "next/server";

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { token } = body;

    if (!token) {
      return NextResponse.json({ error: 'Token is required' }, { status: 400 });
    }

    // Вызов вашего бэкенда
    // Если sendWithAuth требует заголовки авторизации, их нужно взять из request.headers
    // и передать внутрь, либо функция должна уметь работать с контекстом сервера.
    await sendWithAuth("/fcm/push-token", "POST", { token });

    return NextResponse.json({ success: true }, { status: 200 });
  } catch (error) {
    console.error('Error forwarding FCM token:', error);
    return NextResponse.json({ error: 'Internal Server Error' }, { status: 500 });
  }
}