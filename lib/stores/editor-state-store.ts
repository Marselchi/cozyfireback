import { createStore } from "zustand/vanilla";
import type { Descendant } from "slate";
import type { DocumentMetadata } from "@/types/editor-layout";
import {
  type Draft,
  getDrafts as getDraftsUtil,
  saveDraft,
  deleteDraft as deleteDraftUtil,
  getDraft as getDraftUtil,
  getDraftByForId as getDraftByForIdUtil,
  createEmptyValue as createEmptyValueUtil,
} from "@/lib/utils/draft-utils";

const SAVE_INTERVAL = 5000;

const EMPTY_METADATA: DocumentMetadata = {
  name: "Без названия",
  description: "",
  date: "",
  selectedTagsId: [],
  selectedRolesId: [],
};

/** Mirrors InlineEditorElement's shape loosely so the store doesn't need to
 * import slate element types directly — walked out of previewValue. */
export interface InlineEditorNode {
  type: "inline-editor";
  id?: string;
  realId?: string;
  [key: string]: unknown;
}

export interface InlineBlocksDiff {
  addedBlocks: InlineEditorNode[];
  updatedBlocks: InlineEditorNode[];
  deletedBlockIds: string[];
}

export interface EditorStateValues {
  previewValue: Descendant[];
  currentDraftId: string | null;
  currentDraftMetadata: DocumentMetadata;
  currentForId: number | undefined;
  roomName: string;
  /** Local Slate `id`s of inline-editor blocks touched since the last save. */
  dirtyInlineBlockIds: Record<string, true>;
  /** Server `realId`s of inline-editor blocks removed since the last save. */
  deletedInlineBlockRealIds: string[];
}

export interface EditorStateActions {
  setPreviewValueDebounced: (next: Descendant[], ms?: number) => void;
  resetPreviewStore: (next?: Descendant[]) => void;
  loadDraft: (id: string) => void;
  createBlankDraft: () => string;
  createDraftForId: (
    forId: number,
    metadata: DocumentMetadata,
    content: Descendant[],
  ) => string;
  getCurrentDraftId: () => string | null;
  getCurrentDraftMetadata: () => DocumentMetadata;
  updateDraftMetadata: (metadata: Partial<DocumentMetadata>) => void;
  setForId: (forId: number | undefined) => void;
  forceSaveWithMetadata: (metadata: DocumentMetadata) => void;
  getDrafts: () => Draft[];
  deleteDraft: (id: string) => void;
  getDraft: (id: string) => Draft | undefined;
  getDraftByForId: (forId: number) => Draft | undefined;
  deleteCurrentDraft: () => void;
  /** Call with the inline-editor's local `id` on any content/roles change.
   * Never call this for a pure move — position isn't content. */
  markInlineBlockDirty: (id: string | undefined) => void;
  /** Call with the block's `realId` (if any) right after removeInlineEditor. */
  markInlineBlockDeleted: (realId: string | undefined) => void;
  /** Call after a successful server save — the server now reflects the
   * current state, so nothing is dirty/deleted relative to it anymore. */
  resetInlineBlockTracking: () => void;
  /** Walk a Slate tree (pass previewValue) and bucket every inline-editor
   * node into added/updated/deleted per the dirty/deleted tracking above. */
  getInlineBlocksDiff: (tree: Descendant[]) => InlineBlocksDiff;
  /** Internal — called on Provider unmount. Not part of the public consumer API. */
  _cleanupTimers: () => void;
}

export type EditorStateStore = EditorStateValues & EditorStateActions;

export type EditorStateStoreApi = ReturnType<typeof createEditorStateStore>;

interface CreateEditorStateStoreInit {
  roomName: string;
}

/**
 * Factory instead of a single module-level store: each EditorStateProvider
 * mount gets its own instance, so state never bleeds across rooms/documents
 * or across multiple providers mounted at once (e.g. React Strict Mode,
 * split-pane editors, etc).
 *
 * Timers (previewTimer/saveTimer) and the "pending" value live as plain
 * closure variables here instead of useRef — they're already exactly as
 * stable as a ref, but now every action reads live state via get() instead
 * of needing a shadow ref + a useEffect to keep that ref in sync. That
 * removes the currentDraftIdRef / currentDraftMetadataRef / currentForIdRef /
 * previewValueRef dance from the old Context implementation entirely.
 */
export function createEditorStateStore(init: CreateEditorStateStoreInit) {
  let previewTimer: ReturnType<typeof setTimeout> | null = null;
  let saveTimer: ReturnType<typeof setTimeout> | null = null;
  let pendingPreviewValue: Descendant[] | null = null;

  return createStore<EditorStateStore>()((set, get) => {
    // NOTE: assumes `Draft` (in draft-utils) has been extended with
    // `dirtyInlineBlockIds?: string[]` and `deletedInlineBlockRealIds?: string[]`.
    // Stored as a plain string[] (not the Record used in live state) since
    // that's what actually needs to round-trip through storage.
    const inlineTrackingForSave = () => {
      const { dirtyInlineBlockIds, deletedInlineBlockRealIds } = get();
      return {
        dirtyInlineBlockIds: Object.keys(dirtyInlineBlockIds),
        deletedInlineBlockRealIds: [...deletedInlineBlockRealIds],
      };
    };

    const saveCurrentDraftNow = () => {
      const {
        currentDraftId,
        currentDraftMetadata,
        currentForId,
        roomName,
        previewValue,
      } = get();
      if (!currentDraftId) return;

      const content = pendingPreviewValue ?? previewValue;

      saveDraft({
        id: currentDraftId,
        roomName,
        content,
        metadata: currentDraftMetadata,
        updatedAt: Date.now(),
        forId: currentForId,
        ...inlineTrackingForSave(),
      });
    };

    const scheduleAutoSave = () => {
      if (!get().currentDraftId) return;

      if (saveTimer) clearTimeout(saveTimer);
      saveTimer = setTimeout(() => {
        saveCurrentDraftNow();
      }, SAVE_INTERVAL);
    };

    const clearTimers = () => {
      if (saveTimer) {
        clearTimeout(saveTimer);
        saveTimer = null;
      }
      if (previewTimer) {
        clearTimeout(previewTimer);
        previewTimer = null;
      }
      pendingPreviewValue = null;
    };

    // No nesting: an inline-editor's own `content` is a separate embedded
    // document and is never walked for further inline-editor nodes.
    const collectInlineEditors = (
      nodes: Descendant[],
      acc: InlineEditorNode[] = [],
    ): InlineEditorNode[] => {
      for (const node of nodes) {
        if (typeof node !== "object" || node === null) continue;
        const n = node as Record<string, unknown>;
        if (n.type === "inline-editor") {
          acc.push(n as unknown as InlineEditorNode);
          continue;
        }
        if (Array.isArray(n.children)) {
          collectInlineEditors(n.children as Descendant[], acc);
        }
      }
      return acc;
    };

    return {
      previewValue: createEmptyValueUtil(),
      currentDraftId: null,
      currentDraftMetadata: EMPTY_METADATA,
      currentForId: undefined,
      roomName: init.roomName,
      dirtyInlineBlockIds: {},
      deletedInlineBlockRealIds: [],

      setPreviewValueDebounced: (next, ms = 180) => {
        pendingPreviewValue = next;

        if (previewTimer) clearTimeout(previewTimer);

        previewTimer = setTimeout(() => {
          pendingPreviewValue = null;
          set({ previewValue: next });
          scheduleAutoSave();
        }, ms);
      },

      resetPreviewStore: (next) => {
        const v = next ?? createEmptyValueUtil();
        if (previewTimer) {
          clearTimeout(previewTimer);
          previewTimer = null;
        }
        pendingPreviewValue = null;
        set({ previewValue: v });
      },

      loadDraft: (id) => {
        const draft = getDraftUtil(get().roomName, id);
        if (!draft) return;

        clearTimers();

        set({
          currentDraftId: id,
          currentForId: draft.forId,
          currentDraftMetadata: draft.metadata,
          previewValue: draft.content,
          dirtyInlineBlockIds: Object.fromEntries(
            (draft.dirtyInlineBlockIds ?? []).map((blockId) => [
              blockId,
              true as const,
            ]),
          ),
          deletedInlineBlockRealIds: draft.deletedInlineBlockRealIds ?? [],
        });
      },

      createBlankDraft: () => {
        const { roomName } = get();
        const id = Date.now().toString();
        const meta: DocumentMetadata = { ...EMPTY_METADATA };
        const content = createEmptyValueUtil();

        clearTimers();

        set({
          currentDraftId: id,
          currentForId: undefined,
          currentDraftMetadata: meta,
          previewValue: content,
          dirtyInlineBlockIds: {},
          deletedInlineBlockRealIds: [],
        });

        saveDraft({
          id,
          roomName,
          content,
          metadata: meta,
          updatedAt: Date.now(),
          dirtyInlineBlockIds: [],
          deletedInlineBlockRealIds: [],
        });

        return id;
      },

      createDraftForId: (forId, metadata, content) => {
        const { roomName } = get();
        const existing = getDraftByForIdUtil(roomName, forId);

        const id = existing ? existing.id : Date.now().toString();
        const draftContent = existing ? existing.content : content;
        const draftMeta = existing ? existing.metadata : metadata;

        clearTimers();

        set({
          currentDraftId: id,
          currentForId: forId,
          currentDraftMetadata: draftMeta,
          previewValue: draftContent,
          dirtyInlineBlockIds: Object.fromEntries(
            (existing?.dirtyInlineBlockIds ?? []).map((blockId) => [
              blockId,
              true as const,
            ]),
          ),
          deletedInlineBlockRealIds: existing?.deletedInlineBlockRealIds ?? [],
        });

        if (!existing) {
          saveDraft({
            id,
            roomName,
            content: draftContent,
            metadata: draftMeta,
            updatedAt: Date.now(),
            forId,
            dirtyInlineBlockIds: [],
            deletedInlineBlockRealIds: [],
          });
        }

        return id;
      },

      getCurrentDraftId: () => get().currentDraftId,
      getCurrentDraftMetadata: () => get().currentDraftMetadata,

      updateDraftMetadata: (metadata) => {
        set((state) => ({
          currentDraftMetadata: {
            ...state.currentDraftMetadata,
            ...metadata,
            name:
              metadata.name ||
              state.currentDraftMetadata.name ||
              "Черновик " + Date.now().toString(),
          },
        }));

        scheduleAutoSave();
      },

      setForId: (forId) => set({ currentForId: forId }),

      forceSaveWithMetadata: (metadata) => {
        const { currentDraftId, roomName, currentForId } = get();
        if (!currentDraftId) return;

        if (previewTimer) {
          clearTimeout(previewTimer);
          previewTimer = null;
        }

        let contentToSave = get().previewValue;
        if (pendingPreviewValue) {
          contentToSave = pendingPreviewValue;
          pendingPreviewValue = null;
          set({ previewValue: contentToSave });
        }

        set({ currentDraftMetadata: metadata });

        if (saveTimer) {
          clearTimeout(saveTimer);
          saveTimer = null;
        }

        saveDraft({
          id: currentDraftId,
          roomName,
          content: contentToSave,
          metadata,
          updatedAt: Date.now(),
          forId: currentForId,
          ...inlineTrackingForSave(),
        });
      },

      getDrafts: () => getDraftsUtil(get().roomName),

      deleteDraft: (id) => deleteDraftUtil(get().roomName, id),

      getDraft: (id) => getDraftUtil(get().roomName, id),

      getDraftByForId: (forId) => getDraftByForIdUtil(get().roomName, forId),

      deleteCurrentDraft: () => {
        const { currentDraftId, roomName } = get();
        if (!currentDraftId) return;

        try {
          deleteDraftUtil(roomName, currentDraftId);
        } catch (error) {
          console.error("Error deleting current draft:", error);
        }

        clearTimers();

        set({
          currentDraftId: null,
          currentDraftMetadata: EMPTY_METADATA,
          currentForId: undefined,
          previewValue: createEmptyValueUtil(),
          dirtyInlineBlockIds: {},
          deletedInlineBlockRealIds: [],
        });
      },

      markInlineBlockDirty: (id) => {
        if (!id) return;
        set((state) => ({
          dirtyInlineBlockIds: { ...state.dirtyInlineBlockIds, [id]: true },
        }));
        scheduleAutoSave();
      },

      markInlineBlockDeleted: (realId) => {
        if (!realId) return;
        set((state) =>
          state.deletedInlineBlockRealIds.includes(realId)
            ? state
            : {
                deletedInlineBlockRealIds: [
                  ...state.deletedInlineBlockRealIds,
                  realId,
                ],
              },
        );
        scheduleAutoSave();
      },

      resetInlineBlockTracking: () => {
        set({ dirtyInlineBlockIds: {}, deletedInlineBlockRealIds: [] });
      },

      getInlineBlocksDiff: (tree) => {
        const { dirtyInlineBlockIds, deletedInlineBlockRealIds } = get();
        const blocks = collectInlineEditors(tree);

        const addedBlocks: InlineEditorNode[] = [];
        const updatedBlocks: InlineEditorNode[] = [];

        for (const block of blocks) {
          if (!block.realId) {
            addedBlocks.push(block);
          } else if (block.id && dirtyInlineBlockIds[block.id]) {
            updatedBlocks.push(block);
          }
        }

        return {
          addedBlocks,
          updatedBlocks,
          deletedBlockIds: [...deletedInlineBlockRealIds],
        };
      },

      _cleanupTimers: () => {
        clearTimers();
      },
    };
  });
}
