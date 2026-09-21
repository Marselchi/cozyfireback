import { Suspense } from "react";
import { EditorPageContent } from "@/components/editor-layout/editor-page-content";
import { extractIdFromSlug } from "@/lib/server-room-utils";
import { UUID } from "node:crypto";

interface PageProps {
  params: Promise<{ roomName: string; id: string }>;
  searchParams: Promise<{ fresh: UUID }>;
}

export default function DraftPage({
  params,
  searchParams,
}: Readonly<PageProps>) {
  return (
    <Suspense fallback={<div />}>
      <DraftPageContent params={params} searchParams={searchParams} />
    </Suspense>
  );
}

async function DraftPageContent({
  params,
  searchParams,
}: {
  params: Promise<{ roomName: string; id: string }>;
  searchParams: Promise<{ fresh: UUID }>;
}) {
  const { roomName, id } = await params;
  const roomId = extractIdFromSlug(roomName);
  const { fresh } = await searchParams;
  return <EditorPageContent roomName={roomId} key={fresh} draftId={id} />;
}
