"use client";

import type React from "react";
import { useCallback, useRef, useState } from "react";
import { Percent } from "lucide-react";
import { Toggle } from "@/components/ui/toggle";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useSkillList } from "@/lib/blocks/queries";
import { useRoomId } from "@/lib/room-utils";
import type { ChanceMetadata, SpoilerType } from "@/types/editor";

export type ChanceConfigChangePatch = {
  spoilerType: SpoilerType;
  chance?: ChanceMetadata;
};

export type ChanceConfig = {
  id: string;
  spoilerType?: SpoilerType;
  chance?: ChanceMetadata;
  /**
   * Local Slate mutation only — never calls the network. Rides along with
   * the block's own dirty-tracking and is sent to the server on the next
   * real save, exactly like roles.
   */
  onChange: (patch: ChanceConfigChangePatch) => void;
};

// Same pattern as every other toolbar item (mark-item.tsx, block-item.tsx,
// expandable-item.tsx): stop the click from stealing the editor's Slate
// selection. Safe on buttons/toggles/selects because they open on `click`,
// which still fires even when mousedown's default was prevented.
const stopMouseDown = (e: React.MouseEvent) => {
  e.preventDefault();
  e.stopPropagation();
};

// The threshold <input> is the one control that actually NEEDS mousedown's
// default behavior — that's what focuses the input and places the caret.
// `preventDefault()` (used everywhere else above) silently blocks that, so
// this only stops the event from bubbling up to the toolbar/popover's own
// stopMouseDown, without preventing the input's own default. This — not
// `disabled` — was the entire reason the field was inert before.
const allowInputFocus = (e: React.MouseEvent) => {
  e.stopPropagation();
};

export function ChanceControls({ config }: Readonly<{ config: ChanceConfig }>) {
  const {
    spoilerType: initialSpoilerType,
    chance: initialChance,
    onChange,
  } = config;
  const roomId = useRoomId();
  const [open, setOpen] = useState(false);

  const [spoilerType, setSpoilerType] = useState<SpoilerType>(
    initialSpoilerType ?? "normal",
  );
  const [skill, setSkill] = useState(initialChance?.skill ?? "");
  const [threshold, setThreshold] = useState(
    initialChance?.threshold != null ? String(initialChance.threshold) : "",
  );

  // Only fetch the skill list once the popover is actually open on a
  // "Шанс" spoiler — not for every inline-editor block in the document.
  const { data: skills = [] } = useSkillList(
    roomId,
    open && spoilerType === "chance",
  );

  const commitTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const commit = useCallback(
    (type: SpoilerType, nextSkill: string, nextThreshold: string) => {
      if (type === "normal") {
        onChange({ spoilerType: "normal" });
        return;
      }
      const numericThreshold = Number(nextThreshold);
      if (
        !nextSkill ||
        !Number.isFinite(numericThreshold) ||
        numericThreshold <= 0
      ) {
        return; // don't write an incomplete chance config
      }
      onChange({
        spoilerType: "chance",
        chance: { skill: nextSkill, threshold: numericThreshold },
      });
    },
    [onChange],
  );

  const handleTypeChange = (value: SpoilerType) => {
    setSpoilerType(value);
    if (value === "normal") {
      setSkill("");
      setThreshold("");
    }
    commit(value, skill, threshold);
  };

  const handleSkillChange = (value: string) => {
    setSkill(value);
    commit(spoilerType, value, threshold);
  };

  // Debounced, same as every other text-driven update in the editor.
  const handleThresholdChange = (value: string) => {
    setThreshold(value);
    if (commitTimer.current) clearTimeout(commitTimer.current);
    commitTimer.current = setTimeout(() => {
      commit(spoilerType, skill, value);
    }, 300);
  };

  const isChance = spoilerType === "chance";

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Toggle
          className="border border-input w-16 h-6"
          size="sm"
          pressed={isChance}
          aria-label="Настройки спойлера"
          onMouseDown={stopMouseDown}
        >
          Тип
        </Toggle>
      </PopoverTrigger>

      {/* side="right": opens beside the button, not inline in the
          toolbar's own flex row — that inline-flow was why the fields used
          to wrap onto a second line ("spawn below") once the skill +
          threshold controls appeared. */}
      <PopoverContent
        side="right"
        align="start"
        className="flex w-56 flex-col gap-2 p-2"
        onMouseDown={stopMouseDown}
      >
        <Select
          value={spoilerType}
          onValueChange={(value) => handleTypeChange(value as SpoilerType)}
        >
          <SelectTrigger className="h-7 w-full text-xs">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="normal">Обычный</SelectItem>
            <SelectItem value="chance">Шанс</SelectItem>
          </SelectContent>
        </Select>

        {isChance && (
          <>
            <Select value={skill} onValueChange={handleSkillChange}>
              <SelectTrigger className="h-7 w-full text-xs">
                <SelectValue placeholder="Навык" />
              </SelectTrigger>
              <SelectContent>
                {skills.map((item) => (
                  <SelectItem key={item.key} value={item.key}>
                    {item.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Input
              type="number"
              min={1}
              value={threshold}
              onChange={(e) => handleThresholdChange(e.target.value)}
              onMouseDown={allowInputFocus}
              placeholder="Порог"
              aria-label="Порог"
              className="h-7 w-full text-xs"
            />
          </>
        )}
      </PopoverContent>
    </Popover>
  );
}

export function ChanceControlsFallback() {
  return <div className="h-6 w-8" aria-hidden="true" />;
}
