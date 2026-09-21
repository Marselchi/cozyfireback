import { Suspense } from "react";
import { EditorLayout } from "./editor-layout";
import { getAllTags } from "@/server/tags/tags";
import { getAllRolesEdit } from "@/server/roles/roles";
import { getLoreByIdEdit } from "@/server/lore/lore";
import { EditorContent } from "@/types/editor";

interface EditorPageContentProps {
  roomName: string;
  id?: number;
  draftId?: string;
}

async function EditorDataLoader({
  roomName,
  id,
  draftId,
}: Readonly<EditorPageContentProps>) {
  const [tags, roles] = await Promise.all([
    getAllTags(roomName),
    getAllRolesEdit(roomName),
  ]);
  const data = id && !draftId ? await getLoreByIdEdit(id, roomName) : undefined;
  const editorContent: EditorContent | undefined = data
    ? {
        ...data?.content,
        roles: roles,
      }
    : {
        roles: roles,
      };

  return (
    <EditorLayout
      tags={tags}
      roles={roles}
      editorContent={editorContent}
      initialMetadata={data?.metadata}
      id={id}
      draftId={draftId}
    />
  );
}

function EditorLoadingFallback() {
  return (
    <div className="flex h-screen w-full items-center justify-center bg-background">
      <div className="flex flex-col items-center gap-4">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        <p className="text-muted-foreground">Загрузка редактора...</p>
      </div>
    </div>
  );
}

export function EditorPageContent({
  roomName,
  id,
  draftId,
}: Readonly<EditorPageContentProps>) {
  return (
    <Suspense fallback={<EditorLoadingFallback />}>
      <EditorDataLoader id={id} roomName={roomName} draftId={draftId} />
    </Suspense>
  );
}
