"use client";
import { DiceFormulaNode } from "@/lib/editor/renders/dice-formula";

export function Demo() {
  return (
    <div>
      {DiceFormulaNode({ count: 2, sides: 6, modifier: 2 })} атака, а для
      РуСсКиХ {DiceFormulaNode({ count: 4, sides: 3, modifier: -2 })}
    </div>
  );
}
