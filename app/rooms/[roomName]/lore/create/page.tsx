import { Suspense } from "react";
import { EditorPageContent } from "@/components/editor-layout/editor-page-content";
import { extractIdFromSlug } from "@/lib/server-room-utils";
import { FreshKeyWrapper } from "../[id]/edit/author/fresh-key-wrapper";

interface PageProps {
  params: Promise<{ roomName: string }>;
}

export default function CreateLorePage({ params }: PageProps) {
  return (
    <Suspense fallback={<div />}>
      <CreateLorePageContent params={params} />
    </Suspense>
  );
}

async function CreateLorePageContent({
  params,
}: {
  params: Promise<{ roomName: string }>;
}) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);

  return (
    <FreshKeyWrapper>
      <EditorPageContent roomName={roomId} />
    </FreshKeyWrapper>
  );
}
