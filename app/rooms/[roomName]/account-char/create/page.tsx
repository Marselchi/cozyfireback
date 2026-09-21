import { Suspense } from "react";
import { CreateCharacterClient } from "@/components/account-char/create-character-client";
import { extractIdFromSlug } from "@/lib/server-room-utils";

interface Props {
  params: Promise<{ roomName: string }>;
}

export default function CreateCharacterPage({ params }: Readonly<Props>) {
  return (
    <main className="min-h-screen bg-background">
      <div className="mx-auto max-w-3xl px-4 py-8">
        <header className="mb-8">
          <h1 className="text-2xl font-bold tracking-tight text-foreground">
            Новый персонаж
          </h1>
        </header>
        <Suspense>
          <CreateCharacterContent params={params} />
        </Suspense>
      </div>
    </main>
  );
}

async function CreateCharacterContent({
  params,
}: {
  params: Promise<{ roomName: string }>;
}) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  return <CreateCharacterClient roomId={roomId} />;
}
