import { ChanceMetadata, InlineMetadata } from "@/types/editor";
import MarkdownIt from "markdown-it";
import type StateBlock from "markdown-it/lib/rules_block/state_block.mjs";

const OPEN_RE = /^:::\s*restrictedBlock(?:\s+(\{.*\}))?\s*$/;
const CLOSE_RE = /^:::\s*$/;

export default function restrictedBlockPlugin(md: MarkdownIt): void {
  md.block.ruler.before(
    "fence",
    "restricted_block",
    (
      state: StateBlock,
      startLine: number,
      endLine: number,
      silent: boolean,
    ) => {
      const start = state.bMarks[startLine] + state.tShift[startLine];
      const max = state.eMarks[startLine];
      const firstLine = state.src.slice(start, max).trim();

      const openMatch = OPEN_RE.exec(firstLine);
      if (!openMatch) return false;

      if (silent) return true;

      let metadata: InlineMetadata | undefined;

      if (openMatch[1]) {
        try {
          const parsed: unknown = JSON.parse(openMatch[1]);

          if (isInlineMetadata(parsed)) {
            metadata = parsed;
          }
        } catch {
          metadata = undefined;
        }
      }

      let nextLine = startLine + 1;
      let found = false;

      while (nextLine < endLine) {
        const lineStart = state.bMarks[nextLine] + state.tShift[nextLine];
        const lineEnd = state.eMarks[nextLine];
        const line = state.src.slice(lineStart, lineEnd);
        if (CLOSE_RE.test(line.trim())) {
          found = true;
          break;
        }
        nextLine++;
      }

      if (!found) return false;

      const open = state.push("restricted_block_open", "div", 1);
      open.block = true;
      open.meta = { metadata };
      open.map = [startLine, nextLine + 1];

      state.md.block.tokenize(state, startLine + 1, nextLine);

      const close = state.push("restricted_block_close", "div", -1);
      close.block = true;
      close.map = [nextLine, nextLine + 1];

      state.line = nextLine + 1;
      return true;
    },
    { alt: ["paragraph", "reference", "blockquote", "list"] },
  );
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isChanceMetadata(value: unknown): value is ChanceMetadata {
  if (!isRecord(value)) {
    return false;
  }

  return (
    typeof value.skill === "string" &&
    value.skill.length > 0 &&
    typeof value.threshold === "number" &&
    Number.isFinite(value.threshold) &&
    value.threshold > 0
  );
}

function isInlineMetadata(value: unknown): value is InlineMetadata {
  if (!isRecord(value)) {
    return false;
  }

  if (value.id !== undefined && typeof value.id !== "string") {
    return false;
  }

  if (
    value.roles !== undefined &&
    (!Array.isArray(value.roles) ||
      !value.roles.every((role): role is string => typeof role === "string"))
  ) {
    return false;
  }

  if (value.spoilerType !== "normal" && value.spoilerType !== "chance") {
    return false;
  }

  if (value.spoilerType === "chance") {
    return isChanceMetadata(value.chance);
  }

  return true;
}
