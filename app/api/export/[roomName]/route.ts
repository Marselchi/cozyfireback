// app/api/rooms/[roomId]/lore/export/route.ts
import { NextRequest, NextResponse } from 'next/server';
import { getAccessToken } from '@/lib/auth/authUtils';

export async function GET(
  request: NextRequest,
  { params }: { params: { roomName: string } }
) {
  const { searchParams } = new URL(request.url);
  const saveTags = searchParams.get('saveTags');
  const saveSpoilers = searchParams.get('saveSpoilers');
  const dmOnly = searchParams.get('dmOnly');
  const token = await getAccessToken()
  const {roomName} = await params
  
  // 2. Формируем запрос к Spring Backend
  const backendUrl = `${process.env.API_BASE_URL}/rooms/${roomName}/export`;
  const queryParams = new URLSearchParams({
    saveTags: saveTags || 'false',
    saveSpoilers: saveSpoilers || 'false',
    dmOnly: dmOnly || 'false',
  });

  try {
    const response = await fetch(`${backendUrl}?${queryParams.toString()}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`, 
      },
    });

    if (!response.ok) {
      const errorText = await response.text();
      return new NextResponse(errorText || 'Export failed', { status: response.status });
    }

    // 3. Получаем данные как Blob/ArrayBuffer
    const data = await response.arrayBuffer();
    
    // 4. Формируем ответ для клиента с заголовками для скачивания
    const headers = new Headers();
    headers.set('Content-Type', 'application/zip'); // Или тот, что вернул бэкенд
    headers.set('Content-Disposition', 'attachment; filename="lore-export.zip"');
    headers.set('Content-Length', data.byteLength.toString());

    return new NextResponse(data, {
      status: 200,
      headers,
    });

  } catch (error) {
    console.error('Proxy export error:', error);
    return new NextResponse('Internal Server Error', { status: 500 });
  }
}