"use client";

import React, { createContext, useContext, useEffect, useRef } from "react";
import { useStore } from "zustand";
import { useShallow } from "zustand/react/shallow";
import { useRoomId } from "@/lib/room-utils";
import {
  createEditorStateStore,
  type EditorStateStore,
  type EditorStateStoreApi,
} from "@/lib/stores/editor-state-store";

const EditorStoreContext = createContext<EditorStateStoreApi | null>(null);

interface EditorStateProviderProps {
  children: React.ReactNode;
  openDraftId?: string;
}

export function EditorStateProvider({
  children,
  openDraftId,
}: Readonly<EditorStateProviderProps>) {
  const roomName = useRoomId();

  // Created once per Provider mount (per room/document), not a module-level
  // singleton — see createEditorStateStore's doc comment for why.
  const storeRef = useRef<EditorStateStoreApi | null>(null);
  storeRef.current ??= createEditorStateStore({ roomName });

  // roomName can change without the Provider remounting (client-side nav
  // between rooms) — keep the store's copy in sync.
  useEffect(() => {
    storeRef.current?.setState({ roomName });
  }, [roomName]);

  useEffect(() => {
    if (openDraftId) {
      storeRef.current?.getState().loadDraft(openDraftId);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [openDraftId]);

  useEffect(() => {
    const store = storeRef.current;
    return () => {
      store?.getState()._cleanupTimers();
    };
  }, []);

  return (
    <EditorStoreContext.Provider value={storeRef.current}>
      {children}
    </EditorStoreContext.Provider>
  );
}

/**
 * Drop-in replacement for the old `useEditorState()`.
 *
 * Called with no arguments (as every existing consumer does today), it
 * behaves exactly like the old Context: you get the whole state object and
 * re-render whenever any of it changes. No call sites need to change.
 *
 * New consumers that only care about a slice can opt into a selector to
 * avoid re-rendering on unrelated changes, e.g.:
 *
 *   const previewValue = useEditorState((s) => s.previewValue);
 *   const { deleteCurrentDraft, forceSaveWithMetadata } = useEditorState(
 *     (s) => ({ deleteCurrentDraft: s.deleteCurrentDraft, forceSaveWithMetadata: s.forceSaveWithMetadata })
 *   );
 */
export function useEditorState<T = EditorStateStore>(
  selector: (state: EditorStateStore) => T = (s) => s as unknown as T,
): T {
  const store = useContext(EditorStoreContext);
  if (!store) {
    throw new Error("useEditorState must be used within EditorStateProvider");
  }
  return useStore(store, useShallow(selector));
}

/**
 * Non-reactive escape hatch: use this for values/actions you only need
 * inside an event handler (onClick, onSubmit, etc.), never in JSX. Calling
 * `.getState()` on the returned api reads the current values without
 * subscribing the component to them — no re-render when they change.
 *
 *   const editorStateApi = useEditorStateApi();
 *   const handleSave = () => {
 *     const { previewValue, getInlineBlocksDiff } = editorStateApi.getState();
 *     ...
 *   };
 */
export function useEditorStateApi(): EditorStateStoreApi {
  const store = useContext(EditorStoreContext);
  if (!store) {
    throw new Error(
      "useEditorStateApi must be used within EditorStateProvider",
    );
  }
  return store;
}
