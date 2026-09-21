"use client";

import { CharacterOrigin } from "@/types/account-char";
import { TriangleAlertIcon } from "lucide-react";

interface Props {
  origin: string;
}

export function OriginWarningBanner({ origin }: Readonly<Props>) {
  if (origin !== "import") return null;

  return (
    <div
      role="status"
      aria-live="polite"
      className="flex items-start gap-3 rounded-lg border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-900 dark:border-amber-700 dark:bg-amber-950/40 dark:text-amber-200"
    >
      <TriangleAlertIcon
        className="mt-0.5 h-4 w-4 shrink-0 text-amber-600 dark:text-amber-400"
        aria-hidden="true"
      />
      <p>
        <strong className="font-semibold">Импортированный персонаж.</strong>{" "}
        Изменения которые вы делаете здесь не будут обновлены в источнике. Не
        забудьте обновить
      </p>
    </div>
  );
}
