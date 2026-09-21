"use server";
import "server-only";
import { redirect } from "next/navigation";
import {
  CharacterRequest,
  CharacterEditResponse,
  CharacterInlineResponse,
  CharacterListResponse,
  CharacterListFilter,
  PaginatedCharactersResponse,
} from "@/types/characterApi";
import { PageableParams } from "@/types/springTypes";
import {
  buildQueryString,
  getWithAuth,
  sendWithAuth,
} from "@/lib/auth/apiClient";
import { parseMarkdownToSlate } from "@/lib/editor-utils";
import { DEFAULT_STATUS_FIELDS, CoreStatusField } from "@/types/characters";
import { EditorContent } from "@/types/editor";
import { DocumentMetadata } from "@/types/editor-layout";

// Parse status string "name:value;name:value" into statusFields array
function parseStatusString(statusString: string): CoreStatusField[] {
  if (!statusString) {
    // Return default status fields with empty values
    return DEFAULT_STATUS_FIELDS.map((f) => ({ ...f, value: "" }));
  }

  const statusMap = new Map<string, string>();

  statusString.split(";").forEach((block) => {
    const colonIndex = block.indexOf(":");
    if (colonIndex !== -1) {
      const name = block.substring(0, colonIndex).trim();
      const value = block.substring(colonIndex + 1).trim();
      statusMap.set(name, value);
    }
  });

  // Build status fields: first use default fields, then add any extra ones
  const statusFields: CoreStatusField[] = DEFAULT_STATUS_FIELDS.map((f) => ({
    ...f,
    value: statusMap.get(f.label.toLowerCase()) || "",
  }));

  // Add any status fields that are not in defaults
  for (const [name, value] of statusMap.entries()) {
    const exists = DEFAULT_STATUS_FIELDS.some(
      (f) => f.id === name || f.label.toLowerCase() === name.toLowerCase(),
    );
    if (!exists) {
      statusFields.push({
        id: name.toLowerCase(),
        label: name.charAt(0).toUpperCase() + name.slice(1),
        value,
      });
    }
  }

  return statusFields;
}

// Convert statusFields array back to "name:value;name:value" string
function serializeStatusFields(statusFields: CoreStatusField[]): string {
  return statusFields
    .filter((f) => f.value) // Only include fields with values
    .map((f) => `${f.label.toLowerCase()}:${f.value}`)
    .join(";");
}

export async function getCharacterByIdEdit(
  id: number,
  roomName: string,
): Promise<{ content: EditorContent; metadata: DocumentMetadata } | null> {
  const [error, data] = await getWithAuth<CharacterEditResponse>(
    `/characters/${roomName}/${id}/edit`,
  );

  if (error || !data) {
    console.error(error);
    return null;
  }

  const slateContent = parseMarkdownToSlate(data.content);

  const content: EditorContent = {
    id: data.id,
    content: slateContent,
    roles: [],
  };

  const metadata: DocumentMetadata = {
    name: data.name,
    description: data.description,
    date: new Date().toISOString().split("T")[0],
    selectedTagsId: [],
    selectedRolesId: data.roles.map((role) => role.id),
  };

  return { content, metadata };
}

export async function getCharacterByIdUser(id: number, roomName: string) {
  const [error, data] = await getWithAuth<CharacterInlineResponse>(
    `/characters/${roomName}/${id}/view`,
  );

  if (error || !data) {
    console.error(error);
    return null;
  }

  return {
    id: data.id,
    name: data.name,
    description: data.description,
    status: data.status,
    content: data.content,
    isAuthor: data.isAuthor,
    accountName: data.accountName,
    createdByRoomCreator: data.createdByRoomCreator,
    roles: data.roles,
    excerpts: data.excerpts,
    questionCount: data.questionCount,
  };
}

export async function getPaginatedCharacters(
  roomName: string,
  filter: CharacterListFilter = {},
  pageable: PageableParams = {},
): Promise<{
  content: Array<{
    id: number;
    name: string;
    description: string;
    date: string;
    author: string;
    byAdmin: boolean;
    statusBlocks: Array<{ label: string; value: string }>;
  }>;
  page?: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}> {
  const queryString = buildQueryString(filter, pageable);
  const [error, data] = await getWithAuth<PaginatedCharactersResponse>(
    `/characters/${roomName}/all?${queryString || ""}`,
  );

  if (error) {
    console.error(error);
    return { content: [] };
  }

  const content = Array.isArray(data?.content)
    ? data.content.map((c: CharacterListResponse) => ({
        id: c.id,
        name: c.name,
        description: c.description,
        date: new Date().toISOString().split("T")[0],
        author: c.accountName,
        byAdmin: c.createdByRoomCreator,
        statusBlocks: parseStatusString(c.status).map((f) => ({
          label: f.label,
          value: f.value,
        })),
      }))
    : [];

  const page = data?.page
    ? {
        size: data.page.size,
        number: data.page.number,
        totalElements: data.page.totalElements,
        totalPages: data.page.totalPages,
      }
    : undefined;

  return { content, page };
}

export async function createCharacter(payload: {
  roomName: string;
  name: string;
  description?: string;
  status?: string;
  content?: string;
  roleIds?: number[];
}): Promise<{ success: boolean; id: number | null }> {
  try {
    const request: CharacterRequest = {
      name: payload.name,
      description: payload.description,
      status: payload.status,
      content: payload.content,
      roleIds: payload.roleIds,
    };

    const [error, id] = await sendWithAuth<number>(
      `/characters/${payload.roomName}`,
      "POST",
      request,
    );

    if (error) {
      console.error(error);
      return { success: false, id: null };
    }

    return { success: true, id };
  } catch (e) {
    console.error("createCharacter error:", e);
    return { success: false, id: null };
  }
}

export async function updateCharacter(payload: {
  id: number;
  roomName: string;
  name?: string;
  description?: string;
  status?: string;
  content?: string;
  roleIds?: number[];
}): Promise<{ success: boolean }> {
  try {
    const request: CharacterRequest = {
      name: payload.name!,
      description: payload.description,
      status: payload.status,
      content: payload.content,
      roleIds: payload.roleIds,
    };

    const [error] = await sendWithAuth(
      `/characters/${payload.roomName}/${payload.id}`,
      "PATCH",
      request,
      { expectsJson: false },
    );

    if (error) {
      console.error(error);
      return { success: false };
    }

    return { success: true };
  } catch (e) {
    console.error("updateCharacter error:", e);
    return { success: false };
  }
}

export async function deleteCharacter(
  id: number,
  roomName: string,
): Promise<{ success: boolean }> {
  try {
    const [error] = await sendWithAuth(
      `/characters/${roomName}/${id}`,
      "DELETE",
      null,
      { expectsJson: false },
    );

    if (!error) {
      redirect(`/rooms/${roomName}/characters`);
    }

    return { success: !error };
  } catch (e) {
    console.error("deleteCharacter error:", e);
    return { success: false };
  }
}
