"use client";

import { useEffect } from "react";
import { notFound } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { TextHeader } from "@/components/lore/text-header";
import { TextContent } from "@/components/lore/text-content";
import { LoreView } from "@/components/lore/lore-view";
import type { TextData } from "@/types/text";
import { LoadingPage } from "@/components/utils/loading-page";

interface LoreItemViewProps {
  loreId: number;
  roomName: string;
}

interface LoreItemInline {
  id: number;
  title: string;
  description: string;
  date: string;
  content: string;
  author: string;
  isAuthor: boolean;
  nonPublic: boolean;
  byAdmin: boolean;
  tags: { id: number; name: string }[];
  excerpts: unknown[];
  viewCount: number;
  questionCount: number;
}

async function fetchLoreItem(
  roomName: string,
  loreId: number,
): Promise<LoreItemInline | null> {
  const res = await fetch(
    `/api/lore/${loreId}?roomName=${encodeURIComponent(roomName)}`,
    { method: "GET", headers: { "Content-Type": "application/json" } },
  );

  if (res.status === 404) return null;
  if (!res.ok) {
    throw new Error(`Failed to fetch lore item: ${res.status}`);
  }

  return res.json();
}

export function LoreItemView({
  loreId,
  roomName,
}: Readonly<LoreItemViewProps>) {
  const { data, isLoading } = useQuery({
    queryKey: ["lore", roomName, Number(loreId)],
    queryFn: () => fetchLoreItem(roomName, loreId),
    staleTime: 5 * 60 * 1000,
  });

  // No server-side generateMetadata anymore (content is auth-restricted,
  // so it was never going to appear in link previews or search) — set the
  // tab title client-side instead, once the item resolves.
  useEffect(() => {
    if (data?.title) {
      document.title = data.title;
    }
  }, [data?.title]);

  if (isLoading) {
    return <LoadingPage fullScreen={false} />;
  }

  if (!data) {
    notFound();
  }

  const textData: TextData = {
    author: data.author,
    byAdmin: data.byAdmin,
    date: data.date,
    description: data.description,
    id: data.id,
    nonPublic: data.nonPublic,
    tags: data.tags,
    title: data.title,
    isAuthor: data.isAuthor,
    viewCount: data.viewCount,
  };

  return (
    <LoreView
      loreId={loreId}
      questionCount={data.questionCount}
      headerSlot={<TextHeader textData={textData} />}
      contentSlot={
        <TextContent
          content={data.content}
          mdOptions={{ anchorClickable: false, showExcerpts: true }}
        />
      }
    />
  );
}
