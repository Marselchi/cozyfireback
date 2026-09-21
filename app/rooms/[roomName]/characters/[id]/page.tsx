import { Suspense } from "react";
import { CharacterContent } from "@/components/characters/character-content";
import { extractIdFromSlug } from "@/lib/server-room-utils";
import { getAllRolesEdit } from "@/server/roles/roles";

interface CharacterPageProps {
  params: Promise<{ roomName: string; id: string }>;
}

export default function CharacterPage({ params }: CharacterPageProps) {
  return (
    <Suspense fallback={<div />}>
      <CharacterPageContent params={params} />
    </Suspense>
  );
}

async function CharacterPageContent({
  params,
}: {
  params: Promise<{ roomName: string; id: string }>;
}) {
  const { roomName, id } = await params;
  const roomId = extractIdFromSlug(roomName);
  const roles = await getAllRolesEdit(roomName);

  return (
    <CharacterContent
      characterId={parseInt(id)}
      roomName={roomId}
      roles={roles}
    />
  );
}
