import MarkdownIt from "markdown-it";
import type StateInline from "markdown-it/lib/rules_inline/state_inline.mjs";

/**
 * Inline dice formula token: {2d6+5} or {7к2+2}
 *   count    — number of dice to roll
 *   d / к    — separator; "к" (Cyrillic ka, "кубик") is accepted as an
 *              alias for "d" so both {2d6+5} and {2к6+5} work
 *   sides    — number of sides per die
 *   modifier — optional trailing +N / -N
 *
 * Lives entirely inline (unlike restrictedBlock/rollable), so it can sit in
 * the middle of a sentence: "...attack {2d6-3} or use another {7к2+2}..."
 */
const DICE_RE = /^\{(\d+)([dDkKкК])(\d+)([+-]\d+)?\}/;

export default function diceInlinePlugin(md: MarkdownIt): void {
  md.inline.ruler.before(
    "text",
    "dice",
    (state: StateInline, silent: boolean) => {
      // Cheap pre-check before running the regex on every "{" in the doc.
      if (state.src.codePointAt(state.pos) !== 0x7b /* "{" */) {
        return false;
      }

      const match = DICE_RE.exec(state.src.slice(state.pos));
      if (!match) return false;

      if (silent) return true;

      const [full, countStr, letterRaw, sidesStr, modifierStr] = match;
      const letter = letterRaw.toLowerCase() === "d" ? "d" : "к";

      const token = state.push("dice", "", 0);
      token.meta = {
        count: Number(countStr),
        sides: Number(sidesStr),
        modifier: modifierStr ? Number(modifierStr) : 0,
        letter,
      };
      token.level = state.level;

      state.pos += full.length;
      return true;
    },
  );
}
