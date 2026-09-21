import { Suspense } from "react";
import { getCampaignData, getRoomData } from "@/server/room/room";
import { EditableCampaignDate } from "@/components/admin/campaign/editable-campaign-date";
import { EditableCampaignName } from "@/components/admin/campaign/editable-campaign-name";
import { EditableCampaignSituation } from "@/components/admin/campaign/editable-campaign-situation";
import { EditableCampaignSession } from "@/components/admin/campaign/editable-campaign-session";
import { extractIdFromSlug } from "@/lib/server-room-utils";

interface PageProps {
  params: Promise<{ roomName: string }>;
}

export default function CampaignPage({ params }: Readonly<PageProps>) {
  return (
    <div className="container mx-auto py-8 px-4 max-w-4xl">
      <div className="space-y-6">
        <Suspense
          fallback={<div className="h-32 animate-pulse bg-muted rounded-lg" />}
        >
          <CampaignPageContent params={params} />
        </Suspense>
      </div>
    </div>
  );
}

async function CampaignPageContent({
  params,
}: {
  params: Promise<{ roomName: string }>;
}) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  const campaignData = await getCampaignData(roomId);
  const name = await getRoomData(roomId);
  return (
    <>
      <EditableCampaignName initialName={name.displayName} roomName={roomId} />
      <EditableCampaignDate
        initialDate={campaignData.currentDate ?? ""}
        roomName={roomId}
      />
      <EditableCampaignSituation
        initialSituation={campaignData.currentSituation ?? ""}
        roomName={roomId}
      />
      <EditableCampaignSession
        initialSession={campaignData.lastSession ?? ""}
        roomName={roomId}
      />
    </>
  );
}
