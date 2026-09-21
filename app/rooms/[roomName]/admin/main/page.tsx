import { Suspense } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import AuthCodeList from "@/components/admin/auth-code-list";
import UserList from "@/components/admin/user-list";
import { Button } from "@/components/ui/button";
import { getAllRoles } from "@/server/roles/roles";
import CustomLink from "@/components/no-prefetch-link";
import { getAllInvitations } from "@/server/invitation/invitation";
import { getAllUsersInRoom } from "@/server/user/user";
import { extractIdFromSlug } from "@/lib/server-room-utils";

function AuthCodeListSkeleton() {
  return (
    <Card>
      <CardContent className="p-6">
        <div className="space-y-4">
          {[...Array(3)].map((_, i) => (
            <Skeleton key={i} className="h-20 w-full" />
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

function UserListSkeleton() {
  return (
    <Card>
      <CardContent className="p-6">
        <div className="space-y-4">
          {[...Array(3)].map((_, i) => (
            <Skeleton key={i} className="h-16 w-full" />
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

export default function AdminPage({
  params,
}: Readonly<{
  params: Promise<{ roomName: string }>;
}>) {
  return (
    <div className="container mx-auto px-4 py-8 max-w-6xl">
      <div className="space-y-8">
        <Suspense fallback={<AuthCodeListSkeleton />}>
          <AuthCodeListWrapper params={params} />
        </Suspense>

        <Suspense fallback={<UserListSkeleton />}>
          <UserListWrapper params={params} />
        </Suspense>
      </div>
    </div>
  );
}

async function AuthCodeListWrapper({
  params,
}: {
  params: Promise<{ roomName: string }>;
}) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  const authCodes = await getAllInvitations(roomId);
  return <AuthCodeList initialCodes={authCodes} />;
}

async function UserListWrapper({
  params,
}: {
  params: Promise<{ roomName: string }>;
}) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  const [users, availableRoles] = await Promise.all([
    getAllUsersInRoom(roomId),
    getAllRoles(roomId),
  ]);
  return <UserList users={users} availableRoles={availableRoles} />;
}
