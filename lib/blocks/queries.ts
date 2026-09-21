"use client";

import {
  useMutation,
  useQueryClient,
  useQuery,
  useSuspenseInfiniteQuery,
  useSuspenseQuery,
} from "@tanstack/react-query";
import { accessKeys, blockKeys, skillKeys } from "./query-keys";
import {
  denyAccess,
  grantAccess,
  resetAccess,
  updateChanceConfig,
} from "../../server/blocks/actions";
import type {
  AccessRecord,
  BlockDetail,
  BlockSummary,
  BlockType,
  ChanceConfig,
  NormalAccessRecord,
  Skill,
} from "@/types/blocks";
import { Paginated } from "@/types/springTypes";
import { unwrapResult } from "../utils/unwrap-result";

const PAGE_SIZE = 10;

async function fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init);
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(body?.error ?? "Something went wrong.");
  }
  return body as T;
}

export function useBlockList(
  roomId: string,
  filters?: {
    type?: BlockType;
    search?: string;
    id?: string;
  },
) {
  const { type, search, id } = filters ?? {};
  return useSuspenseInfiniteQuery({
    queryKey: blockKeys.list(filters),
    queryFn: ({ pageParam }) => {
      const params = new URLSearchParams();
      params.set("page", String(pageParam));
      params.set("size", String(PAGE_SIZE));
      if (type) params.set("type", type);
      // id search and title search are mutually exclusive — id wins if both
      // somehow end up set.
      if (id) {
        params.set("id", id);
      } else if (search) {
        params.set("search", search);
      }
      return fetchJson<Paginated<BlockSummary>>(
        `/api/blocks/${roomId}?${params.toString()}`,
      );
    },
    initialPageParam: 0,
    getNextPageParam: (lastPage) => {
      const hasNextPage = lastPage.page.number < lastPage.page.totalPages - 1;
      return hasNextPage ? lastPage.page.number + 1 : undefined;
    },
  });
}

export function useBlockDetail(roomId: string, blockId: string) {
  return useSuspenseQuery({
    queryKey: blockKeys.detail(blockId),
    queryFn: () => fetchJson<BlockDetail>(`/api/blocks/${roomId}/${blockId}`),
  });
}

export function useAccessList(roomId: string, blockId: string) {
  return useSuspenseQuery({
    queryKey: accessKeys.list(blockId),
    queryFn: () =>
      fetchJson<(AccessRecord | NormalAccessRecord)[]>(
        `/api/blocks/${roomId}/${blockId}/access`,
      ),
  });
}

/**
 * `enabled` defaults to true for the standalone Blocks admin panel (which
 * always wants the list). The inline-editor toolbar's ChanceControls passes
 * `spoilerType === "chance"` so this query — and the network round-trip
 * behind it — only fires for blocks the user has actually set to "Шанс",
 * instead of once per inline-editor block in the document regardless of
 * type.
 */
export function useSkillList(roomId: string, enabled: boolean = true) {
  return useQuery({
    queryKey: skillKeys.list(roomId),
    queryFn: () =>
      fetchJson<Skill[]>(`/api/skills?roomId=${encodeURIComponent(roomId)}`),
    staleTime: Infinity,
    enabled,
  });
}

function patchAccessRecordInCache(
  data: (AccessRecord | NormalAccessRecord)[] | undefined,
  updated: AccessRecord,
) {
  if (!data) return data;
  return data.map((item) => (item.id === updated.id ? updated : item));
}

export function useResetAccess(roomId: string, blockId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (recordId: string) =>
      unwrapResult(await resetAccess(roomId, recordId)),
    onSuccess: (updated) => {
      queryClient.setQueryData<(AccessRecord | NormalAccessRecord)[]>(
        accessKeys.list(blockId),
        (old) => patchAccessRecordInCache(old, updated),
      );
    },
  });
}

export function useGrantAccess(roomId: string, blockId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (recordId: string) =>
      unwrapResult(await grantAccess(roomId, recordId)),
    onSuccess: (updated) => {
      queryClient.setQueryData<(AccessRecord | NormalAccessRecord)[]>(
        accessKeys.list(blockId),
        (old) => patchAccessRecordInCache(old, updated),
      );
    },
  });
}

export function useDenyAccess(roomId: string, blockId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (recordId: string) =>
      unwrapResult(await denyAccess(roomId, recordId)),
    onSuccess: (updated) => {
      queryClient.setQueryData<(AccessRecord | NormalAccessRecord)[]>(
        accessKeys.list(blockId),
        (old) => patchAccessRecordInCache(old, updated),
      );
    },
  });
}

/**
 * Still used by the standalone Blocks admin panel (editing an
 * already-published block's chance config outside the document editor).
 * The live document editor no longer calls this — see ChanceControls.
 */
export function useUpdateChanceConfig(roomId: string, blockId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (patch: Partial<ChanceConfig>) =>
      unwrapResult(await updateChanceConfig(roomId, blockId, patch)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: blockKeys.detail(blockId) });
    },
  });
}
