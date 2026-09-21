"use client";

import { ExportOptions } from "@/types/import-export";

export async function exportRoomAction(
  options: ExportOptions,
  roomName: string,
): Promise<{ success: boolean; message: string }> {
  try {
    // 1. Формируем Query Params
    const params = new URLSearchParams({
      saveTags: String(options.saveTags),
      saveSpoilers: String(options.spoilers === "unwrap"),
      dmOnly: String(options.dmOnly),
    });

    const url = `${process.env.API_BASE_URL}/rooms/${roomName}/lore/export?${params.toString()}`;

    // 2. Используем твою функцию
    // Предполагаем, что getWithAuthRaw возвращает стандартный Response
    const headers: HeadersInit = {
      // Если токен в заголовке:
      // "Authorization": `Bearer ${getAuthToken()}`,
    };

    const data = await fetch(url, {
      method: "GET",
      headers,
      credentials: "include",
    });

    if (!data.ok) {
      // Пытаемся прочитать текст ошибки, если бэкенд вернул JSON или plain text
      const errorText = await data.text();
      throw new Error(errorText || `Export failed: ${data.status}`);
    }

    // 3. Получаем Blob
    const blob = await data.blob();

    // 4. Инициируем скачивание на клиенте
    const downloadUrl = globalThis.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = downloadUrl;
    link.download = "lore-export.zip";

    document.body.appendChild(link);
    link.click();

    // Очистка
    link.remove();
    globalThis.URL.revokeObjectURL(downloadUrl);

    return { success: true, message: "Export completed" };
  } catch (e) {
    console.error("Export error:", e);
    return {
      success: false,
      message: e instanceof Error ? e.message : "Unknown export error",
    };
  }
}
