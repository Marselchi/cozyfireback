import { CharacterSheet } from "@/components/account-char/character-sheet";
import { extractIdFromSlug } from "@/lib/server-room-utils";
import { Suspense } from "react";

interface Props {
  params: Promise<{ roomName: string; characterId: string }>;
}

export default function CharacterSheetPage({ params }: Readonly<Props>) {
  return (
    <main className="min-h-screen bg-background">
      <div className="mx-auto max-w-3xl px-4 py-8">
        <Suspense
          fallback={
            <div className="flex items-center justify-center py-24 text-muted-foreground text-sm">
              Грузим…
            </div>
          }
        >
          <CharacterSheetWithIdContent params={params} />
        </Suspense>
      </div>
    </main>
  );
}

async function CharacterSheetWithIdContent({
  params,
}: {
  params: Promise<{ roomName: string; characterId: string }>;
}) {
  const { roomName, characterId } = await params;
  const roomId = extractIdFromSlug(roomName);
  return <CharacterSheet roomId={roomId} characterId={characterId} />;
}
