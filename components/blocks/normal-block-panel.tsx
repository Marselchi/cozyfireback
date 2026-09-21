import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { AccessList } from "./access-list";
import type { BlockDetail } from "@/types/blocks";
import { RenderMarkdown } from "../editor/render-markdown";

export function NormalBlockPanel({ block }: Readonly<{ block: BlockDetail }>) {
  return (
    <div className="flex flex-col gap-6">
      <div>{RenderMarkdown({ markdown: block.content })}</div>

      {block.roles.length > 0 && (
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs font-medium text-muted-foreground">
            Роли:
          </span>
          {block.roles.map((role) => (
            <Badge
              key={role}
              variant="secondary"
              className="font-mono text-[11px]"
            >
              {role}
            </Badge>
          ))}
        </div>
      )}

      <Separator />

      <div>
        <h3 className="mb-3 text-sm font-medium text-foreground">
          Доступно для
        </h3>
        <AccessList blockId={block.id} />
      </div>
    </div>
  );
}
