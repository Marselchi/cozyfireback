"use server";
import "server-only";
import { redirect } from "next/navigation";
import {
  LoreListFilterParams,
  LoreListResponse,
  LoreRequest,
  LoreResponse,
  LoreUserInlineResponse,
  LoreUserResponse,
  PageableParams,
  Paginated,
} from "@/types/springTypes";
import {
  buildQueryString,
  getWithAuth,
  sendWithAuth,
} from "@/lib/auth/apiClient";
import { Descendant } from "slate";
import { parseMarkdownToSlate, serializeToMarkdown } from "@/lib/editor-utils";
import { DocumentMetadata } from "@/types/editor-layout";
import { EditorContent } from "@/types/editor";
import { LoreItemData } from "@/components/lore/lore-item";
import { LoreSearchPage, LoreSearchParams } from "@/types/search";
import { InlineBlocksDiff } from "@/lib/stores/editor-state-store";
import { mapInlineBlockToPayload } from "../map/map";

export async function getLoreByIdUser(id: number, roomName: string) {
  const [error, data] = await getWithAuth<LoreUserResponse>(
    `/lore/${roomName}/${id}/view`,
  );
  if (error || !data) {
    console.error(error);
    return null;
  }
  const formattedData = {
    id: data.id,
    title: data.title,
    description: data.description,
    date: data.date,
    content: data.content,
    author: data.accountName,
    isAuthor: data.isAuthor,
    nonPublic: data.nonPublic,
    byAdmin: data.createdByRoomCreator,
    tags: data.tags,
    viewCount: data.viewCount,
  };
  return formattedData;
}

export async function getLoreByIdUserInline(id: number, roomName: string) {
  const [error, data] = await getWithAuth<LoreUserInlineResponse>(
    `/lore/${roomName}/${id}/view`,
  );
  if (error || !data) {
    console.error(error);
    return null;
  }

  const formattedData = {
    id: data.id,
    title: data.title,
    description: data.description,
    date: data.date,
    content: data.content,
    author: data.accountName,
    isAuthor: data.isAuthor,
    nonPublic: data.nonPublic,
    byAdmin: data.createdByRoomCreator,
    tags: data.tags,
    excerpts: data.excerpts,
    viewCount: data.viewCount,
    questionCount: data.questionCount,
  };
  return formattedData;
}

export async function getLoreByIdEdit(id: number, roomName: string) {
  const [error, data] = await getWithAuth<LoreResponse>(
    `/lore/${roomName}/${id}/edit`,
  );
  if (error || !data) {
    console.error(error);
    return null;
  }

  console.log(data.content);

  const slateContent = parseMarkdownToSlate(data.content);

  const content: EditorContent = {
    id: data.id,
    content: slateContent,
    roles: [],
  };

  const metadata: DocumentMetadata = {
    name: data.title,
    description: data.description,
    date: data.date,
    selectedTagsId: data.tags.map((tag) => tag.id),
    selectedRolesId: data.roles.map((role) => role.id),
  };

  return { content, metadata };
}

export interface PaginatedLoreResult {
  content: LoreItemData[];
  page?: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

export async function getPaginatedLore(
  roomName: string,
  filter: LoreListFilterParams = {},
  pageable: PageableParams = {},
): Promise<PaginatedLoreResult> {
  const queryString = buildQueryString(filter, pageable);
  const [error, data] = await getWithAuth<Paginated<LoreListResponse>>(
    `/lore/${roomName}/all?${queryString || ""}`,
  );
  if (error) {
    console.error(error);
    return { content: [] };
  }

  const content: LoreItemData[] = Array.isArray(data?.content)
    ? data.content.map((l: LoreListResponse) => ({
        id: l.id,
        author: l.accountName,
        date: l.date,
        description: l.description,
        byAdmin: l.createdByRoomCreator,
        nonPublic: l.nonPublic,
        tags: l.tags,
        title: l.title,
        isViewed: l.viewed,
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

export async function createLoreItem(payload: {
  roomName: string;
  metadata: DocumentMetadata;
  content: Descendant[];
  inlineBlocks: InlineBlocksDiff;
}) {
  try {
    const markdown = serializeToMarkdown(payload.content);
    const blockChanges: any = {
      addedBlocks: payload.inlineBlocks.addedBlocks.map((b) =>
        mapInlineBlockToPayload(b, true),
      ),
      updatedBlocks: payload.inlineBlocks.updatedBlocks.map((b) =>
        mapInlineBlockToPayload(b, false),
      ),
      deletedBlockIds: payload.inlineBlocks.deletedBlockIds,
    };

    console.log(
      "Payload Data:",
      JSON.stringify(
        {
          markdown,
          blockChanges,
        },
        null,
        2,
      ),
    );
    const request: LoreRequest = {
      content: markdown,
      title: payload.metadata.name,
      date: payload.metadata.date,
      blockChanges: blockChanges,
      description: payload.metadata.description,
      roleIds: payload.metadata.selectedRolesId,
      tagIds: payload.metadata.selectedTagsId,
    };
    console.log(JSON.stringify(request, null, 2));
    const [error, id] = await sendWithAuth(
      `/lore/${payload.roomName}`,
      "POST",
      request,
    );
    if (error) {
      console.error(error);
      return { success: false, id: null };
    }
    return { success: true, id: id };
  } catch (e) {
    console.error("createLoreItem error:", e);
    return { success: false, id: null };
  }
}

export async function deleteLore(id: string) {
  const [error] = await sendWithAuth(`/lore/${id}`, "DELETE", null, {
    expectsJson: false,
  });
  if (!error) {
    redirect("../");
  }
}

export async function updateLoreItem(payload: {
  id: number;
  roomName: string;
  metadata: DocumentMetadata;
  content: Descendant[];
  inlineBlocks: InlineBlocksDiff;
}) {
  const markdown = serializeToMarkdown(payload.content);
  const blockChanges: any = {
    addedBlocks: payload.inlineBlocks.addedBlocks.map((b) =>
      mapInlineBlockToPayload(b, true),
    ),
    updatedBlocks: payload.inlineBlocks.updatedBlocks.map((b) =>
      mapInlineBlockToPayload(b, false),
    ),
    deletedBlockIds: payload.inlineBlocks.deletedBlockIds,
  };
  const request: LoreRequest = {
    content: markdown,
    blockChanges,
    title: payload.metadata.name,
    date: payload.metadata.date,
    description: payload.metadata.description,
    roleIds: payload.metadata.selectedRolesId,
    tagIds: payload.metadata.selectedTagsId,
  };
  const [error] = await sendWithAuth(
    `/lore/${payload.roomName}/${payload.id}`,
    "PUT",
    request,
    { expectsJson: false },
  );
  if (error) {
    console.error(error);
  }
}

export async function getLoreFullSearch(payload: {
  roomName: string;
  filter: LoreSearchParams;
}): Promise<LoreSearchPage> {
  const queryString = buildQueryString(payload.filter);
  const [error, data] = await getWithAuth(
    `/lore/${payload.roomName}/full-search?${queryString ?? ""}`,
  );

  if (error) {
    console.error(error);
    return { content: [], nextOffset: null, totalElements: 0 };
  }

  return data as LoreSearchPage;
}
