import { cn } from "@/lib/utils";

export type ChangeType = "added" | "changed" | "fixed" | "removed" | "planned";

export interface ChangeGroup {
  type: ChangeType;
  items: string[];
}

interface ChangelogListProps {
  groups: ChangeGroup[];
  className?: string;
}

const typeConfig: Record<ChangeType, { label: string; badge: string }> = {
  added: {
    label: "Добавлено",
    badge:
      "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300",
  },
  changed: {
    label: "Изменено",
    badge: "bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300",
  },
  fixed: {
    label: "Фикс",
    badge:
      "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300",
  },
  removed: {
    label: "Удалено",
    badge: "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300",
  },
  planned: {
    label: "Запланировано",
    badge:
      "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300",
  },
};

export function ChangelogList({
  groups,
  className,
}: Readonly<ChangelogListProps>) {
  return (
    <div className={cn("space-y-5", className)}>
      {groups.map((group, i) => {
        const { label, badge } = typeConfig[group.type];
        return (
          <div key={i}>
            <span
              className={cn(
                "inline-block text-xs font-semibold tracking-wide px-2.5 py-0.5 rounded-full mb-2.5",
                badge,
              )}
            >
              {label}
            </span>
            <ul className="space-y-1.5">
              {group.items.map((item, j) => (
                <li key={j} className="flex gap-2.5 text-sm text-foreground">
                  <span className="text-muted-foreground shrink-0 select-none mt-px">
                    —
                  </span>
                  <span className="leading-relaxed">{item}</span>
                </li>
              ))}
            </ul>
          </div>
        );
      })}
    </div>
  );
}
