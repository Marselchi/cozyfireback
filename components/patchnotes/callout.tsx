import { Info, Lightbulb, TriangleAlert, OctagonAlert } from "lucide-react";
import { cn } from "@/lib/utils";

export type CalloutVariant = "info" | "tip" | "warning" | "danger";

interface CalloutProps {
  /** Visual variant. Defaults to "info". */
  variant?: CalloutVariant;
  /** Optional bold title rendered above the body. */
  title?: string;
  children: React.ReactNode;
  className?: string;
}

const variantMap = {
  info: {
    Icon: Info,
    container:
      "bg-blue-50 border-blue-200 dark:bg-blue-950/30 dark:border-blue-800/60",
    icon: "text-blue-500 dark:text-blue-400",
    title: "text-blue-900 dark:text-blue-200",
    body: "text-blue-800 dark:text-blue-300",
  },
  tip: {
    Icon: Lightbulb,
    container:
      "bg-emerald-50 border-emerald-200 dark:bg-emerald-950/30 dark:border-emerald-800/60",
    icon: "text-emerald-600 dark:text-emerald-400",
    title: "text-emerald-900 dark:text-emerald-200",
    body: "text-emerald-800 dark:text-emerald-300",
  },
  warning: {
    Icon: TriangleAlert,
    container:
      "bg-amber-50 border-amber-200 dark:bg-amber-950/30 dark:border-amber-800/60",
    icon: "text-amber-600 dark:text-amber-400",
    title: "text-amber-900 dark:text-amber-200",
    body: "text-amber-800 dark:text-amber-300",
  },
  danger: {
    Icon: OctagonAlert,
    container:
      "bg-red-50 border-red-200 dark:bg-red-950/30 dark:border-red-800/60",
    icon: "text-red-600 dark:text-red-400",
    title: "text-red-900 dark:text-red-200",
    body: "text-red-800 dark:text-red-300",
  },
} satisfies Record<CalloutVariant, object>;

export function Callout({
  variant = "info",
  title,
  children,
  className,
}: Readonly<CalloutProps>) {
  const { Icon, container, icon, title: titleCls, body } = variantMap[variant];

  return (
    <div
      role="note"
      className={cn("flex gap-3 p-4 rounded-lg border", container, className)}
    >
      <Icon
        className={cn("size-4.5 shrink-0 mt-0.5", icon)}
        aria-hidden="true"
      />
      <div className="min-w-0">
        {title && (
          <p className={cn("font-semibold text-sm mb-1", titleCls)}>{title}</p>
        )}
        <div className={cn("text-sm leading-relaxed", body)}>{children}</div>
      </div>
    </div>
  );
}
