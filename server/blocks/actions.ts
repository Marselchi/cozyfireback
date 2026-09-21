"use server";
import "server-only";
import { FrontApiError, toFrontApiError } from "../../lib/auth/apiError";
import type {
  AccessRecord,
  BlockDetail,
  BlockSummary,
  BlockType,
  ChanceConfig,
  NormalAccessRecord,
  Skill,
  WrappedBlock,
} from "@/types/blocks";
import { getWithAuth, sendWithAuth } from "../../lib/auth/apiClient";
import { Paginated } from "@/types/springTypes";
import { RollResult } from "@/components/lore/dice-roll";
import {
  mapAccessRecord,
  mapNormalAccessRecord,
  RawAccessRecord,
  RawNormalAccessRecord,
} from "./util";

export async function resetAccess(
  roomId: string,
  recordId: string,
): Promise<[FrontApiError | null, AccessRecord | null]> {
  const [error, data] = await sendWithAuth(
    `/blocks/access/${recordId}/reset`,
    "POST",
  );

  if (error) {
    return [toFrontApiError(error), null];
  }

  return [null, data ? mapAccessRecord(data as RawAccessRecord) : null];
}

export async function grantAccess(
  roomId: string,
  recordId: string,
): Promise<[FrontApiError | null, AccessRecord | null]> {
  const [error, data] = await sendWithAuth(
    `/blocks/access/${recordId}/grant`,
    "POST",
  );

  if (error) {
    return [toFrontApiError(error), null];
  }

  return [null, data ? mapAccessRecord(data as RawAccessRecord) : null];
}

export async function denyAccess(
  roomId: string,
  recordId: string,
): Promise<[FrontApiError | null, AccessRecord | null]> {
  const [error, data] = await sendWithAuth(
    `/blocks/access/${recordId}/deny`,
    "POST",
  );

  if (error) {
    return [toFrontApiError(error), null];
  }

  return [null, data ? mapAccessRecord(data as RawAccessRecord) : null];
}

// export async function deleteChanceConfig(
//   roomId: string,
//   blockId: string,
// ): Promise<[FrontApiError | null, null]> {
//   const [error] = await sendWithAuth(
//     `/blocks/${blockId}/${roomId}/chance`,
//     "DELETE",
//   );

//   if (error) return [toFrontApiError(error), null];
//   return [null, null];
// }

export async function updateChanceConfig(
  roomId: string,
  blockId: string,
  patch: Partial<ChanceConfig>,
): Promise<[FrontApiError | null, ChanceConfig | null]> {
  const [error, data] = await sendWithAuth(
    `/blocks/${blockId}/chance`,
    "PATCH",
    patch,
  );

  if (error) {
    return [toFrontApiError(error), null];
  }

  return [null, data];
}

export async function getBlocks(params: {
  page?: number;
  size?: number;
  type?: BlockType;
  search?: string;
  /** Search by exact block id instead of by title. Wins over `search` if both are set. */
  id?: string;
  roomId: string;
}): Promise<[FrontApiError | null, Paginated<BlockSummary> | null]> {
  const searchParams = new URLSearchParams();
  searchParams.set("page", String(params.page ?? 0));
  searchParams.set("size", String(params.size ?? 10));
  if (params.type) searchParams.set("type", params.type);
  if (params.id) {
    searchParams.set("id", params.id);
  } else if (params.search) {
    searchParams.set("search", params.search);
  }

  const [error, data] = await getWithAuth(
    `/blocks/${params.roomId}/list?${searchParams.toString()}`,
  );

  if (error) return [toFrontApiError(error), null];
  return [null, data];
}

export async function getBlockDetail(
  roomId: string,
  blockId: string,
): Promise<[FrontApiError | null, BlockDetail | null]> {
  const [error, data] = await getWithAuth(`/blocks/${roomId}/${blockId}`);

  if (error) return [toFrontApiError(error), null];
  return [null, data];
}

export async function getBlockWrapped(
  roomId: string,
  blockId: string,
): Promise<[FrontApiError | null, WrappedBlock | null]> {
  const [error, data] = await getWithAuth(
    `/blocks/${roomId}/${blockId}/wrapped`,
  );

  if (error) return [toFrontApiError(error), null];
  return [null, data];
}

export async function getAccessList(
  roomId: string,
  blockId: string,
): Promise<
  [FrontApiError | null, (AccessRecord | NormalAccessRecord)[] | null]
> {
  const [error, data] = await getWithAuth<{
    accessType: string;
    records: unknown[];
  }>(`/blocks/${roomId}/${blockId}/access`);

  if (error) {
    return [toFrontApiError(error), null];
  }

  if (!data) {
    return [null, null];
  }

  const accessType = String(data.accessType ?? "").toUpperCase();

  if (accessType === "CHANCE") {
    return [null, (data.records as RawAccessRecord[]).map(mapAccessRecord)];
  }

  return [
    null,
    (data.records as RawNormalAccessRecord[]).map(mapNormalAccessRecord),
  ];
}

export async function getSkills(
  roomId: string,
): Promise<[FrontApiError | null, Skill[] | null]> {
  const [error, data] = await getWithAuth(`/skills/${roomId}`);

  if (error) return [toFrontApiError(error), null];
  return [null, data];
}

export async function rollCheck(
  roomId: string,
  rollId: number,
): Promise<[FrontApiError | null, RollResult | null]> {
  const [error, data] = await sendWithAuth(
    `/blocks/${roomId}/${rollId}/roll`,
    "POST",
  );

  if (error) return [toFrontApiError(error), null];
  return [null, data];
}
