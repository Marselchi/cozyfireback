"use client";

import { Suspense } from "react";
import { Dices, Lock, BookLock } from "lucide-react";
import { ScrollArea } from "@/components/ui/scroll-area";
import { SelectedBlockSkeleton } from "./selected-block-skeleton";
import { useBlockDetail } from "@/lib/blocks/queries";
import { blockTypeRegistry } from "@/lib/blocks/block-type-registry";
import { ErrorBoundary } from "../error-boundary";
import { useRoomId } from "@/lib/room-utils";
import { Button } from "../ui/button";
import Link from "next/link";

function SelectedBlockContent({ blockId }: Readonly<{ blockId: string }>) {
  const roomId = useRoomId();
  const { data: block } = useBlockDetail(roomId, blockId);
  console.log(block);
  const BlockTypeComponent = blockTypeRegistry[block.type];

  return (
    <div className="flex flex-col gap-6 p-6">
      <div>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-medium text-foreground text-balance">
            {block.title}
          </h2>
          {block.type === "chance" ? (
            <span className="flex items-center gap-1.5 rounded-full bg-accent/20 px-2.5 py-1 text-xs font-medium text-accent-foreground">
              <Dices className="size-3.5" />
              Шанс
            </span>
          ) : (
            <span className="flex items-center gap-1.5 rounded-full bg-muted px-2.5 py-1 text-xs font-medium text-muted-foreground">
              <Lock className="size-3.5" />
              Обычный
            </span>
          )}
        </div>
        <Link
          className="mt-1 font-mono text-md text-blue-600 underline"
          href={`../lore/${block.loreId}`}
        >
          {`ID:${block.loreId}`}
        </Link>
      </div>

      <BlockTypeComponent block={block} />
    </div>
  );
}

export function SelectedBlockPanel({
  blockId,
}: Readonly<{ blockId: string | null }>) {
  if (!blockId) {
    return (
      <div className="flex h-full flex-col items-center justify-center gap-3 p-12 text-center">
        <BookLock className="size-8 text-muted-foreground/50" />
        <p className="max-w-xs text-sm text-muted-foreground">
          Выберите блок для просмотра
        </p>
      </div>
    );
  }

  return (
    <ScrollArea className="h-full">
      <ErrorBoundary fallbackTitle="Не смог загрузить этот блок :с">
        <Suspense fallback={<SelectedBlockSkeleton />} key={blockId}>
          <SelectedBlockContent blockId={blockId} />
        </Suspense>
      </ErrorBoundary>
    </ScrollArea>
  );
}
