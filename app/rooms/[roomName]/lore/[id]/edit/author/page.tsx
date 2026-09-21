import { Suspense } from "react";
import { EditorPageContent } from "@/components/editor-layout/editor-page-content";
import { extractIdFromSlug } from "@/lib/server-room-utils";
import { FreshKeyWrapper } from "./fresh-key-wrapper";

interface PageProps {
  params: Promise<{ roomName: string; id: string }>;
}

export default function EditAuthorPage({ params }: Readonly<PageProps>) {
  return (
    <Suspense fallback={<div />}>
      <EditAuthorPageContent params={params} />
    </Suspense>
  );
}

async function EditAuthorPageContent({
  params,
}: {
  params: Promise<{ roomName: string; id: string }>;
}) {
  const { id, roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  const idNum = Number(id);

  if (Number.isNaN(idNum)) {
    throw new TypeError("Invalid ID");
  }

  return (
    <FreshKeyWrapper>
      <EditorPageContent roomName={roomId} id={idNum} />
    </FreshKeyWrapper>
  );
}
