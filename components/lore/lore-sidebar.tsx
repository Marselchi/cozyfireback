"use client";

import { useQuery } from "@tanstack/react-query";
import SearchAndFilterContainer from "@/components/lore/search-and-filter-container";
import type { IdName } from "@/types/springTypes";

async function fetchTags(roomName: string): Promise<IdName[]> {
  const res = await fetch(
    `/api/tags?roomName=${encodeURIComponent(roomName)}`,
    { method: "GET", headers: { "Content-Type": "application/json" } },
  );

  if (!res.ok) {
    throw new Error(`Failed to fetch tags: ${res.status}`);
  }

  return res.json();
}

export default function LoreSidebar({
  roomName,
}: Readonly<{ roomName: string }>) {
  // Cached by roomName under the client's single QueryClient instance
  // (mounted once in the root layout), so leaving /lore and coming back
  // reads from cache instead of re-fetching and re-rendering from scratch.
  const { data: tags = [] } = useQuery({
    queryKey: ["tags", roomName],
    queryFn: () => fetchTags(roomName),
    staleTime: 5 * 60 * 1000,
  });

  return <SearchAndFilterContainer tags={tags} />;
}
