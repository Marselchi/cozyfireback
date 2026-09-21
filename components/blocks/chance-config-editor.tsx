"use client";

import { Suspense, useState } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { ErrorBoundary } from "@/components/error-boundary";
import { useSkillList, useUpdateChanceConfig } from "@/lib/blocks/queries";
import type { ChanceConfig } from "@/types/blocks";
import { useRoomId } from "@/lib/room-utils";

function ChanceConfigForm({
  blockId,
  chance,
}: Readonly<{ blockId: string; chance: ChanceConfig }>) {
  const roomId = useRoomId();
  const { data: skills } = useSkillList(roomId);
  const updateChance = useUpdateChanceConfig(roomId, blockId);

  const [skill, setSkill] = useState(chance.skill);
  const [threshold, setThreshold] = useState(String(chance.threshold));

  const dirty =
    skill !== chance.skill || Number(threshold) !== chance.threshold;
  const thresholdNum = Number(threshold);
  const thresholdValid =
    Number.isFinite(thresholdNum) && thresholdNum >= 1 && thresholdNum <= 30;

  return (
    <div className="flex flex-wrap items-end gap-3">
      <div className="flex flex-col gap-1.5">
        <label
          htmlFor="skill-select"
          className="text-xs font-medium text-muted-foreground"
        >
          Навык
        </label>
        <Select
          name="skill-select"
          value={skill}
          onValueChange={(value) => value && setSkill(value)}
        >
          <SelectTrigger className="h-9 w-40 text-sm">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {skills?.map((s) => (
              <SelectItem key={s.key} value={s.key}>
                {s.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <div className="flex flex-col gap-1.5">
        <label
          htmlFor="value"
          className="text-xs font-medium text-muted-foreground"
        >
          Значение
        </label>
        <Input
          id="value"
          type="number"
          min={1}
          max={30}
          value={threshold}
          onChange={(e) => setThreshold(e.target.value)}
          className="h-9 w-24 text-sm"
        />
      </div>
      <Button
        size="default"
        disabled={!dirty || !thresholdValid || updateChance.isPending}
        onClick={() => updateChance.mutate({ skill, threshold: thresholdNum })}
      >
        {updateChance.isPending ? "Сохраняю..." : "Сохранить"}
      </Button>
      {updateChance.isError && (
        <p className="text-xs text-destructive">{updateChance.error.message}</p>
      )}
    </div>
  );
}

export function ChanceConfigEditor({
  blockId,
  chance,
}: Readonly<{
  blockId: string;
  chance: ChanceConfig;
}>) {
  return (
    <ErrorBoundary fallbackTitle="Couldn't load skills">
      <Suspense fallback={<Skeleton className="h-16 w-64" />}>
        <ChanceConfigForm blockId={blockId} chance={chance} />
      </Suspense>
    </ErrorBoundary>
  );
}
