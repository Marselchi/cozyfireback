import { Suspense } from "react";
import CharacterSkeletonLoader from "@/components/characters/character-skeleton-loader";
import CharacterSidebar from "@/components/characters/character-sidebar";
import CharacterContent from "@/components/characters/character-content-list";
import { extractIdFromSlug } from "@/lib/server-room-utils";

interface CharactersPageProps {
  searchParams: Promise<{ search?: string; admin?: string; page?: string }>;
  params: Promise<{ roomName: string }>;
}

export default function CharactersPage({
  searchParams,
  params,
}: Readonly<CharactersPageProps>) {
  return (
    <div className="flex container mx-auto py-8 px-4 min-h-[calc(100vh-4rem)] min-w-full">
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8 min-w-full min-h-full">
        <div className="lg:col-span-1">
          <Suspense fallback={<div className="mt-6">Загрузка...</div>}>
            <CharacterSidebarWrapper params={params} />
          </Suspense>
        </div>

        <div className="lg:col-span-3 flex flex-col">
          <Suspense fallback={<CharacterSkeletonLoader />}>
            <CharacterContentWrapper
              params={params}
              searchParams={searchParams}
            />
          </Suspense>
        </div>
      </div>
    </div>
  );
}

async function CharacterSidebarWrapper({
  params,
}: {
  params: Promise<{ roomName: string }>;
}) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  return <CharacterSidebar roomName={roomId} />;
}

async function CharacterContentWrapper({
  params,
  searchParams,
}: {
  params: Promise<{ roomName: string }>;
  searchParams: Promise<{ search?: string; admin?: string; page?: string }>;
}) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  const search = await searchParams;
  return (
    <Suspense
      key={JSON.stringify(search)}
      fallback={<CharacterSkeletonLoader />}
    >
      <CharacterContent roomName={roomId} searchParams={search} />
    </Suspense>
  );
}
