import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { AccessStatus } from "@/types/blocks";

const STATUS_LABEL: Record<AccessStatus, string> = {
  pending: "Ожидание",
  fail: "Провал",
  passed: "Прошел",
  granted: "Выдано",
  denied: "Запрещено",
};

const STATUS_CLASS: Record<AccessStatus, string> = {
  pending: "bg-muted text-muted-foreground border-transparent",
  fail: "bg-destructive/10 text-destructive border-destructive/20",
  passed: "bg-primary/15 text-primary border-primary/25",
  granted: "bg-accent/20 text-accent-foreground border-accent/30",
  denied: "bg-destructive/15 text-destructive border-destructive/25",
};

export function StatusBadge({ status }: Readonly<{ status: AccessStatus }>) {
  return (
    <Badge
      variant="outline"
      className={cn("font-mono text-xs font-medium", STATUS_CLASS[status])}
    >
      {STATUS_LABEL[status]}
    </Badge>
  );
}
