import MarkdownIt from "markdown-it";
import type StateInline from "markdown-it/lib/rules_inline/state_inline.mjs";

export default function placeholderPlugin(md: MarkdownIt): void {
  md.inline.ruler.before(
    "text",
    "placeholder",
    (state: StateInline, silent: boolean) => {
      if (
        state.src.codePointAt(state.pos) !== 0x7b ||
        state.src.codePointAt(state.pos + 1) !== 0x7b
      ) {
        return false;
      }

      const end = state.src.indexOf("}}", state.pos + 2);
      if (end === -1) return false;

      if (silent) return true;

      const content = state.src.slice(state.pos + 2, end);
      const token = state.push("placeholder", "", 0);
      token.content = content;
      token.level = state.level;

      state.pos = end + 2;
      return true;
    },
  );
}
