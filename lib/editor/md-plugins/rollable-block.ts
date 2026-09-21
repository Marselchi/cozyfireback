import MarkdownIt from "markdown-it";
import type StateBlock from "markdown-it/lib/rules_block/state_block.mjs";

// Self-closed, single-line block: {rollable id=123}
const ROLLABLE_RE = /^\{rollable\s+id="(\d+)"\}$/;

export default function rollableBlockPlugin(md: MarkdownIt): void {
  md.block.ruler.before(
    "fence",
    "rollable_block",
    (
      state: StateBlock,
      startLine: number,
      endLine: number,
      silent: boolean,
    ) => {
      const start = state.bMarks[startLine] + state.tShift[startLine];
      const max = state.eMarks[startLine];
      const line = state.src.slice(start, max).trim();

      const match = ROLLABLE_RE.exec(line);
      if (!match) return false;

      if (silent) return true;

      const rollId = Number(match[1]);

      // No open/close pair needed — this is a single self-closed token,
      // unlike restricted_block which wraps child content.
      const token = state.push("rollable_block", "div", 0);
      token.block = true;
      token.meta = { rollId };
      token.map = [startLine, startLine + 1];

      state.line = startLine + 1;
      return true;
    },
    { alt: ["paragraph", "reference", "blockquote", "list"] },
  );
}
