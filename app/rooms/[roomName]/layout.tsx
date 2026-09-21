import { getAllRoomsNav, getRoomContext } from "@/server/room/room";
import { getRoomUser } from "@/server/user/user";
import { RoomNavigation } from "@/components/navbar/room-navigation";
import { RoomNavigationSkeleton } from "@/components/navbar/room-skeleton";
import { UserProvider } from "@/lib/contexts/user-context";
import { notFound } from "next/navigation";
import type React from "react";
import { Suspense } from "react";
import { extractIdFromSlug } from "@/lib/server-room-utils";

export default async function RootLayout({
  children,
  params,
}: Readonly<{
  children: React.ReactNode;
  params: Promise<{ roomName: string }>;
}>) {
  return (
    <div>
      <Suspense fallback={<RoomNavigationSkeleton />}>
        <RoomLayoutContent params={params}>{children}</RoomLayoutContent>
      </Suspense>
    </div>
  );
}

async function RoomLayoutContent({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ roomName: string }>;
}) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  const [room, allRooms, roomUser] = await Promise.all([
    getRoomContext(roomId),
    getAllRoomsNav(),
    getRoomUser(roomId),
  ]);

  if (!room) {
    notFound();
  }

  return (
    <UserProvider initialUser={roomUser}>
      <RoomNavigation room={room} allRooms={allRooms} />
      <main className="min-h-[calc(100vh-4rem)]">{children}</main>
    </UserProvider>
  );
}
