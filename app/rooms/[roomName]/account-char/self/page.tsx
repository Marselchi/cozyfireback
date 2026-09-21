import { CharacterSheet } from "@/components/account-char/character-sheet";
import { extractIdFromSlug } from "@/lib/server-room-utils";
import { Suspense } from "react";

interface Props {
  params: Promise<{ roomName: string }>;
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
          <CharacterSheetContent params={params} />
        </Suspense>
      </div>
    </main>
  );
}

async function CharacterSheetContent({
  params,
}: {
  params: Promise<{ roomName: string }>;
}) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  return <CharacterSheet roomId={roomId} />;
}
