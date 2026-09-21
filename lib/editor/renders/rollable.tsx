"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { DiceRoll } from "@/components/lore/dice-roll";
import { useRoomId } from "@/lib/room-utils";
import Link from "next/link";
import { RenderMarkdown } from "@/components/editor/render-markdown";
import { WrappedBlock } from "@/types/blocks";
import CustomLink from "@/components/no-prefetch-link";

type RollStatus = "idle" | "success" | "failure" | "error";

type RollableNodeProps = {
  rollId: number;
};

async function fetchRollableContent(
  roomId: string,
  rollId: number,
): Promise<WrappedBlock> {
  const res = await fetch(`/api/blocks/${roomId}/${rollId}/wrapped`);
  if (!res.ok) {
    const body = (await res.json().catch(() => null)) as {
      error?: string;
    } | null;
    throw new Error(
      body?.error ?? `Failed to fetch rollable content for id ${rollId}`,
    );
  }
  // Server returns raw markdown for the revealed content — RenderMarkdown
  // will run it through parseMarkdownToSlate itself.
  return res.json();
}

export function useRollableResult(roomId: string, rollId: number) {
  return useQuery({
    queryKey: ["rollable", roomId, rollId],
    queryFn: () => fetchRollableContent(roomId, rollId),
    enabled: false, // never fires on mount — only via refetch() on success
    retry: false,
  });
}

export function RollableNode({ rollId }: Readonly<RollableNodeProps>) {
  const [open, setOpen] = useState(false);
  const [status, setStatus] = useState<RollStatus>("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const roomId = useRoomId();

  const { data, isFetching, isError, refetch } = useRollableResult(
    roomId,
    rollId,
  );

  const handleResult = (result: boolean) => {
    setOpen(false);
    setStatus(result ? "success" : "failure");

    if (result) {
      refetch();
    }
  };

  const handleRollError = (message: string) => {
    setOpen(false);
    setErrorMessage(message);
    setStatus("error");
  };

  if (status === "error") {
    return (
      <div className="my-2 rounded border border-red-400 bg-red-50 p-3 text-sm text-red-700 dark:bg-red-950/30 dark:text-red-300">
        Не удалось выполнить бросок.{" "}
        <CustomLink href="../account-char/self" className="underline ">
          Возможно вы не создали персонажа?
        </CustomLink>{" "}
        Обновите страницу и попробуйте снова
      </div>
    );
  }

  // Failed roll — block is gone for good.
  if (status === "failure") {
    return null;
  }

  if (status === "success") {
    if (isFetching) {
      return (
        <div className="my-2 rounded border border-gray-300 p-2 text-sm text-gray-500">
          Загружаем...
        </div>
      );
    }

    if (isError) {
      return (
        <div className="my-2 rounded border border-red-400 p-2 text-sm text-red-600">
          Вупсень пупсень произошел
        </div>
      );
    }

    if (data) {
      return RenderMarkdown({ markdown: data.content });
    }

    return null;
  }

  return (
    <>
      <div
        onClick={() => setOpen(true)}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") setOpen(true);
        }}
        role="button"
        tabIndex={0}
        className="my-2 flex items-center justify-center rounded border border-input bg-card p-2 cursor-pointer"
        suppressHydrationWarning
      >
        <img src="/dice.svg" className="h-20 w-20" alt="" />
      </div>
      <DiceRoll
        open={open}
        onOpenChange={setOpen}
        rollId={rollId}
        onResult={handleResult}
        onError={handleRollError}
      />
    </>
  );
}
