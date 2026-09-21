"use client";

import { useCallback, useState } from "react";
import { Dices, RotateCcw } from "lucide-react";

export interface DiceFormulaNodeProps {
  count: number;
  sides: number;
  modifier: number;
}

interface DiceRollState {
  rolls: number[];
  total: number;
}

const LIMITS = {
  count: { min: 1, max: 100 },
  sides: { min: 2, max: 1000 },
  modifier: { min: -1000, max: 1000 },
};

function clamp(value: number, min: number, max: number): number {
  if (value > max) return max;
  if (value < min) return min;
  return value;
}

function rollDice(count: number, sides: number): number[] {
  const rolls: number[] = [];
  for (let i = 0; i < count; i++) {
    rolls.push(Math.floor(Math.random() * sides) + 1);
  }
  return rolls;
}

function formatModifier(modifier: number): string {
  if (modifier === 0) return "";
  return modifier > 0 ? `+${modifier}` : `${modifier}`;
}

export function formatDiceFormula(
  count: number,
  sides: number,
  modifier: number,
): string {
  return `${count}d${sides}${formatModifier(modifier)}`;
}

export function DiceFormulaNode({
  count,
  sides,
  modifier,
}: Readonly<DiceFormulaNodeProps>) {
  const safeCount = clamp(count, LIMITS.count.min, LIMITS.count.max);
  const safeSides = clamp(sides, LIMITS.sides.min, LIMITS.sides.max);
  const safeModifier = clamp(
    modifier,
    LIMITS.modifier.min,
    LIMITS.modifier.max,
  );

  const [result, setResult] = useState<DiceRollState | null>(null);

  const handleRoll = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();
      e.stopPropagation();
      const rolls = rollDice(safeCount, safeSides);
      const total = rolls.reduce((sum, roll) => sum + roll, 0) + safeModifier;
      setResult({ rolls, total });
    },
    [safeCount, safeSides, safeModifier],
  );

  const formula = formatDiceFormula(safeCount, safeSides, safeModifier);

  return (
    <button
      type="button"
      contentEditable={false}
      onMouseDown={(e) => e.stopPropagation()}
      onClick={handleRoll}
      className="inline-flex select-none items-center gap-1.5 rounded-md border border-input bg-card px-2 py-0.5 align-middle font-mono text-sm text-foreground transition-colors hover:border-primary/50 hover:bg-accent cursor-pointer"
      title="Нажмите, чтобы бросить"
    >
      {result ? (
        <RotateCcw className="h-3 w-3 text-muted-foreground" />
      ) : (
        <Dices className="h-3 w-3 text-muted-foreground" />
      )}
      {result ? (
        <span>
          {formula} = <strong className="text-primary">{result.total}</strong>
          <span className="ml-1 text-xs text-muted-foreground">
            ({result.rolls.join(" + ")}
            {safeModifier !== 0
              ? ` ${safeModifier > 0 ? "+" : "-"} ${Math.abs(safeModifier)}`
              : ""}
            )
          </span>
        </span>
      ) : (
        <span>{formula}</span>
      )}
    </button>
  );
}
