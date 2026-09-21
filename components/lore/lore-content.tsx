"use client";

import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import LoreList from "@/components/lore/lore-list";
import LoreSkeletonLoader from "@/components/lore/lore-skeleton-loader";
import DraftsListWrapper from "./drafts/drafts-list-wrapper";
import type { PaginatedLoreResult } from "@/server/lore/lore";

interface LoreContentProps {
  roomName: string;
}

const PAGE_SIZE = 6;

async function fetchLorePage(params: {
  roomName: string;
  search: string | null;
  tag: string | null;
  admin: string | null;
  viewed: string | null;
  page: number;
  size: number;
}): Promise<PaginatedLoreResult> {
  const query = new URLSearchParams({
    roomName: params.roomName,
    page: String(params.page),
    size: String(params.size),
  });
  if (params.search) query.set("search", params.search);
  if (params.tag) query.set("tag", params.tag);
  if (params.admin) query.set("admin", params.admin);
  if (params.viewed) query.set("viewed", params.viewed);

  const res = await fetch(`/api/lore?${query.toString()}`, {
    method: "GET",
    headers: { "Content-Type": "application/json" },
  });

  if (!res.ok) {
    throw new Error(`Failed to fetch lore list: ${res.status}`);
  }

  return res.json();
}

export default function LoreContent({ roomName }: Readonly<LoreContentProps>) {
  const searchParams = useSearchParams();

  const isDraftsMode = searchParams.get("drafts") === "true";

  const search = searchParams.get("search");
  const tag = searchParams.get("tag");
  const admin = searchParams.get("admin");
  const viewed = searchParams.get("viewed");
  const pageFromUrl = Number(searchParams.get("page") ?? "1");
  const page = pageFromUrl - 1;

  // Hook is always called (rules-of-hooks) — `enabled` just skips the
  // network call while in drafts mode, where this data isn't needed at all.
  //
  // No `placeholderData: keepPreviousData` here on purpose: each
  // filter/page combo is its own cache entry (queryKey), so switching to
  // one visited before resolves instantly from cache with no skeleton,
  // while switching to a genuinely new page/filter correctly shows
  // `isLoading` (the skeleton below) instead of silently reusing stale
  // data from the previous page.
  const { data, isLoading, isError } = useQuery({
    queryKey: [
      "lore-list",
      roomName,
      search,
      tag,
      admin,
      viewed,
      page,
      PAGE_SIZE,
    ],
    queryFn: () =>
      fetchLorePage({
        roomName,
        search,
        tag,
        admin,
        viewed,
        page,
        size: PAGE_SIZE,
      }),
    enabled: !isDraftsMode,
    staleTime: 5 * 60 * 1000,
  });

  if (isDraftsMode) {
    return <DraftsListWrapper />;
  }

  if (isLoading) {
    return <LoreSkeletonLoader />;
  }

  if (isError || !data) {
    return (
      <div className="text-center py-12 text-destructive text-sm">
        Ошибка загрузки лора
      </div>
    );
  }

  return <LoreList initialLoreItems={data.content} page={data.page} />;
}
