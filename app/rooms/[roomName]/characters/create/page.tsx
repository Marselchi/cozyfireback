import { Suspense } from "react";
import { CharacterContent } from "@/components/characters/character-content";
import { extractIdFromSlug } from "@/lib/server-room-utils";
import { getAllRolesEdit } from "@/server/roles/roles";

interface CharacterCreatePageProps {
  params: Promise<{ roomName: string }>;
}

export default function CharacterCreatePage({
  params,
}: CharacterCreatePageProps) {
  return (
    <Suspense fallback={<div />}>
      <CharacterCreateContent params={params} />
    </Suspense>
  );
}

async function CharacterCreateContent({
  params,
}: {
  params: Promise<{ roomName: string }>;
}) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  const roles = await getAllRolesEdit(roomName);

  return <CharacterContent characterId={0} roomName={roomId} roles={roles} />;
}
