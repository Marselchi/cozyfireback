import { cn } from "@/lib/utils";

interface SectionDividerProps {
  /** Optional label centered on the divider line. */
  label?: string;
  className?: string;
}

export function SectionDivider({
  label,
  className,
}: Readonly<SectionDividerProps>) {
  if (!label) {
    return <hr className={cn("border-input", className)} />;
  }

  return (
    <div
      className={cn("flex items-center gap-3", className)}
      role="separator"
      aria-label={label}
    >
      <div className="h-px flex-1 bg-input" />
      <span className="text-[11px] font-semibold text-muted-foreground uppercase tracking-widest whitespace-nowrap">
        {label}
      </span>
      <div className="h-px flex-1 bg-input" />
    </div>
  );
}
