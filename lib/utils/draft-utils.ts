import type { Descendant } from "slate";
import type { DocumentMetadata } from "@/types/editor-layout";

export interface Draft {
  id: string;
  roomName: string;
  content: Descendant[];
  metadata: DocumentMetadata;
  updatedAt: number;
  forId?: number;
  /** Local Slate `id`s of inline-editor blocks edited since the last real save. */
  dirtyInlineBlockIds?: string[];
  /** Server `realId`s of inline-editor blocks removed since the last real save. */
  deletedInlineBlockRealIds?: string[];
}

const MAX_DRAFTS = 10;

function isBrowser(): boolean {
  return typeof window !== "undefined" && typeof localStorage !== "undefined";
}

function getStorageKey(roomName: string) {
  return `slate_drafts_${roomName}`;
}

export function getDrafts(roomName: string): Draft[] {
  if (!isBrowser()) return [];

  try {
    const raw = localStorage.getItem(getStorageKey(roomName));
    return raw ? JSON.parse(raw) : [];
  } catch (e) {
    console.error("Failed to load drafts", e);
    return [];
  }
}

export function saveDraft(draft: Draft): void {
  if (!isBrowser()) return;

  try {
    const drafts = getDrafts(draft.roomName);

    if (draft.forId !== undefined) {
      const i = drafts.findIndex((d) => d.forId === draft.forId);

      if (i !== -1) drafts[i] = { ...draft, updatedAt: Date.now() };
      else drafts.unshift({ ...draft, updatedAt: Date.now() });
    } else {
      const i = drafts.findIndex((d) => d.id === draft.id);
      if (i !== -1) drafts[i] = { ...draft, updatedAt: Date.now() };
      else drafts.unshift({ ...draft, updatedAt: Date.now() });
    }

    localStorage.setItem(
      getStorageKey(draft.roomName),
      JSON.stringify(
        drafts.sort((a, b) => b.updatedAt - a.updatedAt).slice(0, MAX_DRAFTS),
      ),
    );
  } catch (e) {
    console.error("Failed to save draft", e);
  }
}

export function deleteDraft(roomName: string, id: string): void {
  const drafts = getDrafts(roomName).filter((d) => d.id !== id);
  localStorage.setItem(getStorageKey(roomName), JSON.stringify(drafts));
}

export function getDraft(roomName: string, id: string): Draft | undefined {
  return getDrafts(roomName).find((d) => d.id === id);
}

export function getDraftByForId(
  roomName: string,
  forId: number,
): Draft | undefined {
  return getDrafts(roomName).find((d) => d.forId === forId);
}

export const createEmptyValue = (): Descendant[] => [
  { type: "paragraph", children: [{ text: "" }] } as any,
];
