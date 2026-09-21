import MarkdownIt from "markdown-it";

export default function quoteBlockPlugin(md: MarkdownIt): void {
  md.block.ruler.before(
    "paragraph",
    "quote_block",
    (state, startLine, _endLine, silent) => {
      const max = state.eMarks[startLine];

      const lineStart = state.bMarks[startLine];
      const contentStart = lineStart + state.tShift[startLine];
      const indent = state.src.slice(lineStart, contentStart);

      const line = state.src.slice(contentStart, max);
      if (!line.startsWith("<?")) return false;
      if (silent) return true;

      const content = line.slice(2).trim();
      const token = state.push("quote_block", "custom", 0);
      token.content = content;
      token.meta = { indent };
      token.map = [startLine, startLine + 1];

      state.line = startLine + 1;
      return true;
    },
    { alt: ["paragraph", "blockquote", "list"] },
  );
}
