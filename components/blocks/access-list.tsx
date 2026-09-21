"use client";

import { Suspense } from "react";
import { RotateCcw, Check, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorBoundary } from "@/components/error-boundary";
import { StatusBadge } from "./status-badge";
import {
  useAccessList,
  useDenyAccess,
  useGrantAccess,
  useResetAccess,
} from "@/lib/blocks/queries";
import { AccessRecord, NormalAccessRecord } from "@/types/blocks";
import { useRoomId } from "@/lib/room-utils";

function isChanceRecord(
  record: AccessRecord | NormalAccessRecord,
): record is AccessRecord {
  return "status" in record;
}

function AccessListSkeleton() {
  return (
    <div className="flex flex-col gap-2">
      {Array.from({ length: 4 }).map((_, i) => (
        <Skeleton key={i} className="h-12 w-full" />
      ))}
    </div>
  );
}

function ChanceAccessRow({
  record,
  roomId,
  blockId,
}: Readonly<{ record: AccessRecord; roomId: string; blockId: string }>) {
  const reset = useResetAccess(roomId, blockId);
  const grant = useGrantAccess(roomId, blockId);
  const deny = useDenyAccess(roomId, blockId);

  const canGrant = record.status !== "passed" && record.status !== "granted";
  const canDeny = record.status !== "denied";
  const pending = reset.isPending || grant.isPending || deny.isPending;

  return (
    <div className="flex items-center justify-between gap-3 rounded-lg border border-border bg-card px-3 py-2.5">
      <div className="flex min-w-0 flex-1 items-center gap-3">
        <span className="truncate text-sm text-foreground">
          {record.userName}
        </span>
        <StatusBadge status={record.status} />
        {record.rollValue !== undefined && (
          <span className="font-mono text-xs text-muted-foreground">
            Реузльтат: {record.rollValue}
          </span>
        )}
      </div>
      <div className="flex items-center gap-1">
        <Button
          size="icon"
          variant="ghost"
          className="size-7"
          title="Сбросить"
          disabled={pending}
          onClick={() => reset.mutate(record.id)}
        >
          <RotateCcw className="size-3.5" />
        </Button>
        <Button
          size="icon"
          variant="ghost"
          className="size-7 text-primary hover:text-primary disabled:opacity-30"
          title={canGrant ? "Выдать доступ" : "Доступ уже получен"}
          disabled={pending || !canGrant}
          onClick={() => grant.mutate(record.id)}
        >
          <Check className="size-3.5" />
        </Button>
        <Button
          size="icon"
          variant="ghost"
          className="size-7 text-destructive hover:text-destructive disabled:opacity-30"
          title={canDeny ? "Запретить" : "Уже запрещено"}
          disabled={pending || !canDeny}
          onClick={() => deny.mutate(record.id)}
        >
          <X className="size-3.5" />
        </Button>
      </div>
    </div>
  );
}

function NormalAccessRow({ record }: Readonly<{ record: NormalAccessRecord }>) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-lg border border-border bg-card px-3 py-2.5">
      <span className="truncate text-sm text-foreground">
        {record.userName}
      </span>
      <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
        <Check className="size-3.5 text-primary" />
        Есть доступ
      </span>
    </div>
  );
}

function AccessListContent({
  blockId,
}: Readonly<{
  blockId: string;
}>) {
  const roomId = useRoomId();
  const { data: records } = useAccessList(roomId, blockId);

  if (records.length === 0) {
    return (
      <p className="py-6 text-center text-sm text-muted-foreground">
        Ни один пользователь не имеет доступа к этому блоку.
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      {records.map((record) =>
        isChanceRecord(record) ? (
          <ChanceAccessRow
            key={record.id}
            record={record}
            roomId={roomId}
            blockId={blockId}
          />
        ) : (
          <NormalAccessRow key={record.id} record={record} />
        ),
      )}
    </div>
  );
}

export function AccessList({
  blockId,
}: Readonly<{
  blockId: string;
}>) {
  return (
    <div className="flex flex-col gap-3">
      <ErrorBoundary fallbackTitle="Не могу получить доступ">
        <Suspense fallback={<AccessListSkeleton />}>
          <AccessListContent blockId={blockId} />
        </Suspense>
      </ErrorBoundary>
    </div>
  );
}
