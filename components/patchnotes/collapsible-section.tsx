"use client";

import { useState } from "react";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";

interface CollapsibleSectionProps {
  /** Button label shown when collapsed and expanded */
  label: string;
  children: React.ReactNode;
  /** Whether to start open. Defaults to false. */
  defaultOpen?: boolean;
  className?: string;
}

export function CollapsibleSection({
  label,
  children,
  defaultOpen = false,
  className,
}: Readonly<CollapsibleSectionProps>) {
  const [open, setOpen] = useState(defaultOpen);

  return (
    <div
      className={cn(
        "rounded-lg border border-border overflow-hidden",
        className,
      )}
    >
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        className="w-full flex items-center justify-between gap-3 px-4 py-3 bg-muted/60 hover:bg-muted transition-colors text-sm font-medium text-left cursor-pointer"
      >
        <span className="text-foreground">{label}</span>
        <ChevronDown
          className={cn(
            "size-4 shrink-0 text-muted-foreground transition-transform duration-200",
            open && "rotate-180",
          )}
          aria-hidden="true"
        />
      </button>

      {open && <div className="border-t border-border">{children}</div>}
    </div>
  );
}
