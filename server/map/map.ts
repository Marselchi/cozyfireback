import { serializeToMarkdown } from "@/lib/editor-utils";
import { InlineEditorNode } from "@/lib/stores/editor-state-store";
import { Descendant } from "slate";

export function mapInlineBlockToPayload(
  block: InlineEditorNode,
  isAdded: boolean,
) {
  const {
    id,
    realId,
    children,
    content,
    roles,
    spoilerType,
    chance,
    type,
    ...restMetadata
  } = block as any;

  const metadata: Record<string, unknown> = { ...restMetadata };

  if (roles !== undefined) metadata.roles = roles;
  if (spoilerType !== undefined) metadata.spoilerType = spoilerType;
  if (chance !== undefined) metadata.chance = chance;

  const payload: any = {
    type,
    content: serializeToMarkdown(content as Descendant[]),
    metadata,
  };

  if (isAdded) {
    payload.localId = id; // id с фронта используется как localId
  } else {
    payload.id = realId; // realId с фронта используется как реальный id в БД
  }

  return payload;
}
