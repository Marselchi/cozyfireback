import { Suspense } from "react";
import { DataTable } from "@/components/admin/data-table";
import { TableSkeleton } from "@/components/admin/table-skeleton";
import { Tags } from "lucide-react";
import { getAllTags } from "@/server/tags/tags";
import { createItem, updateItem, deleteItem } from "@/server/roles/items";
import { extractIdFromSlug } from "@/lib/server-room-utils";

async function TagsTable({
  params,
}: Readonly<{ params: Promise<{ roomName: string }> }>) {
  const { roomName } = await params;
  const roomId = extractIdFromSlug(roomName);
  const tags = await getAllTags(roomId);
  return (
    <DataTable
      data={tags}
      createAction={createItem}
      editAction={updateItem}
      deleteAction={deleteItem}
      itemType="tag"
    />
  );
}

export default function TagsPage({
  params,
}: Readonly<{ params: Promise<{ roomName: string }> }>) {
  return (
    <div className="space-y-6">
      <Suspense fallback={<TableSkeleton rows={5} />}>
        <TagsTable params={params} />
      </Suspense>
    </div>
  );
}
