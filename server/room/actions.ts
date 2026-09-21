"use server";
import "server-only";
import { createRoom } from "@/server/room/room";
import { revalidatePath, updateTag } from "next/cache";
import { z } from "zod";
import slugify from "@sindresorhus/slugify";
import { sendWithAuth, sendWithAuthNoJson } from "@/lib/auth/apiClient";

interface Error {
  error?: string;
  success?: boolean;
}

const roomSchema = z.object({
  name: z
    .string()
    .regex(
      /^[a-zA-Z0-9_-]+$/,
      "Имя пишется на английском, может включать цифры, - и _",
    )
    .max(16, "Имя должно быть не более 16 символов"),
  display_name: z
    .string()
    .max(30, "Отображаемое имя должно быть не более 30 символов")
    .refine(
      (value) => /^[a-zA-Zа-яА-ЯёЁ\s\-]+$/.test(value),
      "Отображаемое имя должно быть на русском или английском, может включать - и _",
    ),
  description: z.string().optional(),
});

export async function createRoomAction(
  previousState: Error,
  formData: FormData,
) {
  try {
    const rawData = {
      name: formData.get("name"),
      display_name: formData.get("display_name"),
      description: formData.get("description") ?? "",
    };

    const validatedData = roomSchema.parse(rawData);

    const roomData = {
      name: slugify(validatedData.name, { lowercase: true }),
      display_name: validatedData.display_name,
      description: validatedData.description,
    };

    await createRoom(roomData);
    updateTag("user-rooms");

    return { success: true };
  } catch (error) {
    if (error instanceof z.ZodError) {
      //todo: fix

      return {
        error: error.issues.map((err) => err.message).join(", "),
        success: false,
      };
    }

    return { error: `Произошла внутренняя ошибка: ${error}`, success: false };
  }
}

export async function updateCampaignName(
  roomName: string,
  previousState: Error,
  formData: FormData,
) {
  const name = formData.get("name") as string;

  if (!name || name.trim().length === 0) {
    return { success: false, error: "Без названия нельзя" };
  }

  const [error] = await sendWithAuth(
    `/rooms/${roomName}/name`,
    "PUT",
    {
      name: name,
    },
    { expectsJson: false },
  );
  if (error) {
    return { success: false, error: "Чето нето, напиши мне" };
  }
  revalidatePath("/campaign");
  return { success: true, error: "Успешно обновлено" };
}

export async function updateCampaignDate(
  roomName: string,
  previousState: Error,
  formData: FormData,
) {
  const currentDate = formData.get("currentDate") as string;
  if (!currentDate) {
    return { success: false, error: "Пустую дату нельзя" };
  }
  const [error] = await sendWithAuth(
    `/rooms/${roomName}/details`,
    "PUT",
    {
      date: currentDate,
    },
    { expectsJson: false },
  );
  if (error) {
    return { success: false, error: "Чето нето, напиши мне" };
  }
  revalidatePath("/campaign");
  return { success: true, error: "Успешно обновлено" };
}

export async function updateCampaignSituation(
  roomName: string,
  previousState: Error,
  formData: FormData,
) {
  const currentSituation = formData.get("currentSituation") as string;
  if (!currentSituation || currentSituation.trim().length === 0) {
    return { success: false, error: "Пустую ситуацию нельзя(" };
  }

  const [error] = await sendWithAuth(
    `/rooms/${roomName}/details`,
    "PUT",
    {
      situation: currentSituation,
    },
    { expectsJson: false },
  );
  if (error) {
    return { success: false, error: "Чето нето, напиши мне" };
  }
  revalidatePath("/campaign");
  return { success: true, error: "Успешно обновлено" };
}

export async function updateCampaignSession(
  roomName: string,
  previousState: Error,
  formData: FormData,
) {
  const currentSession = formData.get("currentSession") as string;

  if (!currentSession || currentSession.trim().length === 0) {
    return { success: false, error: "Пустуое нельзя(" };
  }

  const [error] = await sendWithAuth(
    `/rooms/${roomName}/details`,
    "PUT",
    {
      lastSession: currentSession,
    },
    { expectsJson: false },
  );
  if (error) {
    return { success: false, error: "Чето нето, напиши мне" };
  }
  revalidatePath("/campaign");
  return { success: true, error: "Успешно обновлено" };
}

import { ImportOptions } from "@/types/import-export";

export async function importRoomAction(
  file: FormData,
  options: ImportOptions,
  roomName: string,
): Promise<{ success: boolean; message: string }> {
  file.append("filetype", options.fileType);
  file.append("roomReset", String(options.replaceWholeRoom));
  file.append(
    "replaceOnConflict",
    String(options.conflictStrategy === "replace"),
  );
  file.append("autolink", String(options.tryAutolink));
  const [error, data] = await sendWithAuthNoJson(
    `/rooms/${roomName}/lore/import`,
    "POST",
    file,
    { expectsJson: false },
  );
  return { success: true, message: "Import completed (mock)" };
}
