import type { BlockType } from "@/types/blocks";

export const blockKeys = {
  all: ["blocks"] as const,
  lists: () => [...blockKeys.all, "list"] as const,
  list: (filters?: { type?: BlockType; search?: string; id?: string }) =>
    [...blockKeys.lists(), filters ?? {}] as const,
  details: () => [...blockKeys.all, "detail"] as const,
  detail: (id: string) => [...blockKeys.details(), id] as const,
};

export const accessKeys = {
  all: ["access"] as const,
  lists: () => [...accessKeys.all, "list"] as const,
  list: (blockId: string) => [...accessKeys.lists(), blockId] as const,
};

export const skillKeys = {
  all: ["skills"] as const,
  lists: () => [...skillKeys.all, "list"] as const,
  list: (roomId: string) => [...skillKeys.lists(), roomId] as const,
};
