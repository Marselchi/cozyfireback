"use server";
import "server-only";
import { EditorTemplate } from "@/types/editor-templates";
import {
  buildQueryString,
  getWithAuth,
  sendWithAuth,
} from "@/lib/auth/apiClient";
import { PageableParams, Paginated } from "@/types/springTypes";

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface TemplateListFilter {
  search?: string;
}

export interface PaginatedTemplateResult {
  content: EditorTemplate[];
  page?: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

// ---------------------------------------------------------------------------
// Server functions
// ---------------------------------------------------------------------------

export async function searchTemplates(
  query: string = "",
  page: number = 0,
  pageSize: number = 10,
): Promise<PaginatedTemplateResult> {
  const filter: TemplateListFilter = query.trim()
    ? { search: query.trim() }
    : {};
  const pageable: PageableParams = { page, size: pageSize };
  const queryString = buildQueryString(filter, pageable);

  const [error, data] = await getWithAuth<Paginated<EditorTemplate>>(
    `/templates?${queryString || ""}`,
  );
  if (error) {
    console.error(error);
    return { content: [] };
  }

  const content: EditorTemplate[] = Array.isArray(data?.content)
    ? data.content
    : [];

  const pageMeta = data?.page
    ? {
        size: data.page.size,
        number: data.page.number,
        totalElements: data.page.totalElements,
        totalPages: data.page.totalPages,
      }
    : undefined;

  return { content, page: pageMeta };
}

export async function getTemplateContent(
  templateId: number,
): Promise<{ id: number; markdown: string } | null> {
  const [error, data] = await getWithAuth<{ id: number; content: string }>(
    `/templates/${templateId}`,
  );
  if (error || !data) {
    console.error(error);
    return null;
  }

  return {
    id: data.id,
    markdown: data.content,
  };
}

export async function renameTemplate(
  templateId: number,
  newName: string,
): Promise<{ success: boolean }> {
  const [error] = await sendWithAuth(
    `/templates/${templateId}`,
    "PUT",
    { name: newName },
    { expectsJson: false },
  );
  if (error) {
    console.error(error);
    return { success: false };
  }
  return { success: true };
}

export async function deleteTemplate(
  templateId: number,
): Promise<{ success: boolean }> {
  const [error] = await sendWithAuth(
    `/templates/${templateId}`,
    "DELETE",
    null,
    { expectsJson: false },
  );
  if (error) {
    console.error(error);
    return { success: false };
  }
  return { success: true };
}

export async function createTemplate(
  roomName: string,
  name: string,
  content: string,
): Promise<{ success: boolean; id: number | null }> {
  try {
    const [error, data] = await sendWithAuth<{ id: number }>(
      `/templates/${roomName}`,
      "POST",
      { name, content },
    );
    if (error) {
      console.error(error);
      return { success: false, id: null };
    }
    return { success: true, id: data?.id ?? null };
  } catch (e) {
    console.error("createTemplate error:", e);
    return { success: false, id: null };
  }
}
