import { Suspense } from "react";
import LoreSkeletonLoader from "@/components/lore/lore-skeleton-loader";
import LoreSidebar from "@/components/lore/lore-sidebar";
import LoreContent from "@/components/lore/lore-content";
import { extractIdFromSlug } from "@/lib/server-room-utils";

interface LorePageProps {
  params: Promise<{ roomName: string }>;
}

export default function LorePage({ params }: Readonly<LorePageProps>) {
  return (
    <div className="flex container mx-auto py-8 px-4 min-h-[calc(100vh-4rem)] min-w-full">
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8 min-w-full min-h-full">
        <div className="lg:col-span-1">
          <Suspense fallback={<div className="mt-6">Загрузка тегов...</div>}>
            <LoreSidebarWrapper params={params} />
          </Suspense>
        </div>

        <div className="lg:col-span-3 flex flex-col">
          <Suspense fallback={<LoreSkeletonLoader />}>
            <LoreContentWrapper params={params} />
          </Suspense>
        </div>
      </div>
    </div>
  );
}

async function LoreSidebarWrapper({
  params,
}: Readonly<{
  params: Promise<{ roomName: string }>;
}>) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  return <LoreSidebar roomName={roomId} />;
}

async function LoreContentWrapper({
  params,
}: Readonly<{
  params: Promise<{ roomName: string }>;
}>) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  return <LoreContent roomName={roomId} />;
}
