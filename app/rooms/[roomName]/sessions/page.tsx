import { Suspense } from "react";
import { SessionProvider } from "@/components/sessions/session-context";
import { SessionsCalendar } from "@/components/sessions/sessions-calendar";
import { UpcomingSessions } from "@/components/sessions/upcoming-sessions";
import { extractIdFromSlug } from "@/lib/server-room-utils";
import { getAllUsersInRoomIdName } from "@/server/user/user";

interface SessionsPageProps {
  params: Promise<{ roomName: string }>;
}

export default function SessionsPage({ params }: SessionsPageProps) {
  return (
    <SessionProvider>
      <div className="container mx-auto px-4 py-8">
        <div className="flex flex-col lg:flex-row gap-8">
          <div className="flex-1 min-w-0">
            <div className="bg-card rounded-xl border border-border p-4 md:p-6">
              <Suspense
                fallback={
                  <div className="h-96 animate-pulse bg-muted rounded-lg" />
                }
              >
                <SessionsCalendarWrapper params={params} />
              </Suspense>
            </div>
          </div>

          <div className="w-full lg:w-80 shrink-0">
            <Suspense
              fallback={
                <div className="h-64 animate-pulse bg-muted rounded-lg" />
              }
            >
              <UpcomingSessionsWrapper params={params} />
            </Suspense>
          </div>
        </div>
      </div>
    </SessionProvider>
  );
}

async function SessionsCalendarWrapper({
  params,
}: {
  params: Promise<{ roomName: string }>;
}) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  const roomUsers = await getAllUsersInRoomIdName(roomId);
  return <SessionsCalendar roomName={roomId} roomUsers={roomUsers} />;
}

async function UpcomingSessionsWrapper({
  params,
}: {
  params: Promise<{ roomName: string }>;
}) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  return <UpcomingSessions roomName={roomId} />;
}
