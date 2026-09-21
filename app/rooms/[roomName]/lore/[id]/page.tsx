import { extractIdFromSlug } from "@/lib/server-room-utils";
import { LoreItemView } from "@/components/lore/lore-item-view";

interface PageProps {
  params: Promise<{ id: number; roomName: string }>;
}

export default async function TextViewPage({ params }: Readonly<PageProps>) {
  const { id, roomName } = await params;
  const roomId = extractIdFromSlug(roomName);

  return <LoreItemView loreId={id} roomName={roomId} />;
}
