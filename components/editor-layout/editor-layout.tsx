"use client";

import { BlockLayoutProvider } from "./block-layout-context";
import { ResizableBlockGrid } from "./resizable-block-grid";
import { MetadataPanel } from "./metadata-panel";
import type { Tag, Role, DocumentMetadata } from "@/types/editor-layout";
import { EditorContent } from "@/types/editor";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  EditorStateProvider,
  useEditorState,
} from "../editor/editor-state-provider";
import { useParams, useRouter } from "next/navigation";
import { Draft, getDraft, getDraftByForId } from "@/lib/utils/draft-utils";
import { ConfirmDialog } from "../utils/confirm-dialog";
import { useRoomId } from "@/lib/room-utils";
import { useShallow } from "zustand/react/shallow";

interface EditorLayoutBaseProps {
  tags: Tag[];
  roles: Role[];
}

/**
 * 1. Create new lore:  no `id`, no `editorContent`, no `draftId`
 * 2. Edit existing:    `id` + `editorContent` + `initialMetadata`
 * 3. Open draft:       `draftId`
 */
export interface EditorLayoutProps extends EditorLayoutBaseProps {
  id?: number;
  editorContent?: EditorContent;
  initialMetadata?: DocumentMetadata;
  draftId?: string;
}

interface InnerProps extends EditorLayoutProps {
  existingDraftId?: string;
  draft?: Draft;
}

function EditorLayoutInner({
  id,
  editorContent,
  initialMetadata,
  tags,
  roles,
  draft,
  existingDraftId,
}: Readonly<InnerProps>) {
  const router = useRouter();
  const {
    createBlankDraft,
    createDraftForId,
    deleteDraft,
    forceSaveWithMetadata,
    currentDraftId,
    currentDraftMetadata,
  } = useEditorState(
    useShallow((s) => ({
      createBlankDraft: s.createBlankDraft,
      createDraftForId: s.createDraftForId,
      deleteDraft: s.deleteDraft,
      forceSaveWithMetadata: s.forceSaveWithMetadata,
      currentDraftId: s.currentDraftId,
      currentDraftMetadata: s.currentDraftMetadata,
    })),
  );

  const initialized = useRef(false);

  const currentDraftIdRef = useRef<string | null>(currentDraftId);
  const currentDraftMetadataRef =
    useRef<DocumentMetadata>(currentDraftMetadata);

  useEffect(() => {
    currentDraftIdRef.current = currentDraftId;
  }, [currentDraftId]);

  useEffect(() => {
    currentDraftMetadataRef.current = currentDraftMetadata;
  }, [currentDraftMetadata]);

  const [showPrevDialog, setShowPrevDialog] = useState(!!existingDraftId);

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    if (draft) {
      return;
    }

    if (id !== undefined && editorContent?.content) {
      if (!existingDraftId) {
        createDraftForId(
          id,
          initialMetadata ?? emptyMeta(),
          editorContent.content,
        );
      }
    } else {
      createBlankDraft();
    }
  }, [
    draft,
    id,
    editorContent,
    existingDraftId,
    initialMetadata,
    createBlankDraft,
    createDraftForId,
  ]);

  useEffect(() => {
    return () => {
      if (currentDraftIdRef.current) {
        forceSaveWithMetadata(currentDraftMetadataRef.current);
      }
    };
  }, [forceSaveWithMetadata]);

  const handleGoToPreviousChanges = useCallback(() => {
    setShowPrevDialog(false);
    if (existingDraftId) {
      router.push(`../../drafts/${existingDraftId}`);
    }
  }, [existingDraftId, router]);

  const handleDiscardPreviousChanges = useCallback(() => {
    if (existingDraftId) {
      deleteDraft(existingDraftId);
    }
    setShowPrevDialog(false);
    if (id !== undefined && editorContent?.content) {
      createDraftForId(
        id,
        initialMetadata ?? emptyMeta(),
        editorContent.content,
      );
    }
  }, [
    existingDraftId,
    id,
    editorContent,
    initialMetadata,
    deleteDraft,
    createDraftForId,
  ]);

  return (
    <>
      <BlockLayoutProvider>
        <div className="relative h-screen w-full overflow-hidden bg-background">
          <div className="h-full pr-0">
            <ResizableBlockGrid
              editorContent={
                draft
                  ? { id: draft.forId!, content: draft.content, roles: roles }
                  : editorContent
              }
            />
          </div>

          <MetadataPanel
            initialMetadata={draft?.metadata ?? initialMetadata}
            tags={tags}
            roles={roles}
            id={draft?.forId ?? editorContent?.id}
          />
        </div>
      </BlockLayoutProvider>

      <ConfirmDialog
        open={showPrevDialog}
        onOpenChange={setShowPrevDialog}
        onAccept={handleGoToPreviousChanges}
        onCancel={handleDiscardPreviousChanges}
        title="Есть предыдущие изменения"
        description="Вы открыли лор, который уже редактировали. Хотите отменить изменения? Предыдущие изменения будут удалены"
        customAcceptButtonText="Перейти к изменениям"
        customCancelButtonText="Отменить предыдущие изменения"
        customAcceptButtonVariant="secondary"
        confirmButtonType="custom"
      />
    </>
  );
}

function firstParam(value: string | string[] | undefined): string | undefined {
  if (Array.isArray(value)) return value[0];
  return value;
}

export function EditorLayout({
  id,
  editorContent,
  initialMetadata,
  tags,
  roles,
  draftId,
}: Readonly<EditorLayoutProps>) {
  const params = useParams<{
    roomName: string;
    draftId?: string | string[];
  }>();

  const roomName = useRoomId();
  const routeDraftId = firstParam(params.draftId);
  const resolvedDraftId = draftId ?? routeDraftId;

  let existingDraftId: string | undefined;
  let draft: Draft | undefined;

  if (id !== undefined && !resolvedDraftId && roomName) {
    const existing = getDraftByForId(roomName, id);
    if (existing) {
      existingDraftId = existing.id;
    }
  }

  if (resolvedDraftId !== undefined && roomName) {
    draft = getDraft(roomName, resolvedDraftId);
  }

  return (
    <EditorStateProvider openDraftId={resolvedDraftId}>
      <EditorLayoutInner
        id={id}
        editorContent={editorContent}
        initialMetadata={initialMetadata}
        tags={tags}
        roles={roles}
        draft={draft}
        existingDraftId={existingDraftId}
      />
    </EditorStateProvider>
  );
}

function emptyMeta(): DocumentMetadata {
  return {
    name: "Без названия",
    description: "",
    date: "",
    selectedTagsId: [],
    selectedRolesId: [],
  };
}
