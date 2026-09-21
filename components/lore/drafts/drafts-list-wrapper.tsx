"use client";

import { useEffect, useState } from "react";
import DraftsList from "./drafts-list";
import { getDrafts, type Draft } from "../../../lib/utils/draft-utils";
import { useRoomId } from "@/lib/room-utils";

export default function DraftsListWrapper() {
  const [drafts, setDrafts] = useState<Draft[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const roomName = useRoomId();

  useEffect(() => {
    // Get drafts from localStorage
    const allDrafts = getDrafts(roomName);

    // Filter drafts to exclude items with href='lore-list'
    // We check both the metadata name and look for any indication this is a lore-list draft
    const filteredDrafts = allDrafts.filter((draft) => {
      // Exclude drafts where the name or any metadata suggests it's a lore-list view
      const isLoreList =
        draft.metadata.name?.toLowerCase().includes("lore-list") ||
        draft.id?.toLowerCase().includes("lore-list");
      return !isLoreList;
    });

    setDrafts(filteredDrafts);
    setIsLoading(false);
  }, []);

  if (isLoading) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">Загрузка черновиков...</p>
      </div>
    );
  }

  return <DraftsList drafts={drafts} />;
}
