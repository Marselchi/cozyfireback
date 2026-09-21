import { Suspense } from "react";
import { DataTable } from "@/components/admin/data-table";
import { TableSkeleton } from "@/components/admin/table-skeleton";
import { getAllRoles } from "@/server/roles/roles";
import { createItem, deleteItem, updateItem } from "@/server/roles/items";
import { extractIdFromSlug } from "@/lib/server-room-utils";

async function RolesTable({
  params,
}: Readonly<{ params: Promise<{ roomName: string }> }>) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  const roles = await getAllRoles(roomId);
  return (
    <DataTable
      data={roles}
      createAction={createItem}
      editAction={updateItem}
      deleteAction={deleteItem}
      itemType="role"
    />
  );
}

export default function RolesPage({
  params,
}: Readonly<{ params: Promise<{ roomName: string }> }>) {
  return (
    <div className="space-y-6">
      <Suspense fallback={<TableSkeleton rows={4} />}>
        <RolesTable params={params} />
      </Suspense>
    </div>
  );
}
