import { getCampaignData } from "@/server/room/room";
import CampaignDate from "@/components/main-page/campaign-date";
import CampaignSituation from "@/components/main-page/campaign-situation";
import { Suspense } from "react";
import LastSession from "@/components/main-page/last-session";
import { extractIdFromSlug } from "@/lib/server-room-utils";

interface PageProps {
  params: Promise<{ roomName: string }>;
}

export default async function HomePage({ params }: Readonly<PageProps>) {
  return (
    <main className="container mx-auto px-4 py-8">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 min-h-screen">
        <div className="md:col-span-3 space-y-6">
          <Suspense
            fallback={<div className=" bg-muted animate-pulse rounded-lg" />}
          >
            <CampaignContent params={params} />
          </Suspense>
        </div>
      </div>
    </main>
  );
}

async function CampaignContent({
  params,
}: {
  params: Promise<{ roomName: string }>;
}) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  const campaignData = await getCampaignData(roomId);

  return (
    <>
      <CampaignDate date={campaignData?.currentDate} />
      <CampaignSituation situation={campaignData?.currentSituation} />
      <LastSession session={campaignData?.lastSession} />
    </>
  );
}
