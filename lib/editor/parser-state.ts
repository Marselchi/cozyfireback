import type {
  Token,
  ParserContext,
  PossibleInlineChildren,
} from "@/types/node-definition";
import type { Descendant } from "slate";
import type { NodeRegistry } from "./registry";
import { parseInlineTokens } from "./inline";

export class ParserState implements ParserContext {
  tokens: Token[];
  index: number = 0;
  private _registry: NodeRegistry;

  constructor(tokens: Token[], registry: NodeRegistry) {
    this.tokens = tokens;
    this._registry = registry;
  }

  current(): Token | null {
    return this.tokens[this.index] ?? null;
  }

  next(): Token | null {
    return this.tokens[this.index + 1] ?? null;
  }

  peek(offset = 1): Token | null {
    return this.tokens[this.index + offset] ?? null;
  }

  advance(): void {
    this.index++;
  }

  rewind(steps = 1): void {
    this.index = Math.max(0, this.index - steps);
  }

  done(): boolean {
    return this.index >= this.tokens.length;
  }

  parseChildren(until?: string[], stopOnSameLevel = true): Descendant[] {
    const children: Descendant[] = [];
    const startLevel = this.current()?.level ?? 0;

    let lastEndLine = this.current()?.map?.[0] ?? 0;

    while (this.index < this.tokens.length) {
      const before = this.index;
      const token = this.current();
      if (!token) break;

      if (until?.includes(token.type)) break;

      if (
        stopOnSameLevel &&
        token.type.endsWith("_close") &&
        token.level <= startLevel
      ) {
        break;
      }

      // восстановление пустых строк
      if (Array.isArray(token.map)) {
        const blankLines = token.map[0] - lastEndLine;

        for (let i = 0; i < blankLines; i++) {
          children.push({
            type: "paragraph",
            children: [{ text: "" }],
          });
        }
      }

      const result = this._registry.parse(token, this);

      if (Array.isArray(token.map)) {
        lastEndLine = token.map[1];
      }

      if (Array.isArray(result)) {
        children.push(...result);
      } else if (result) {
        children.push(result);
      }

      if (this.index === before) {
        this.advance();
      }
    }

    return children;
  }

  parseInline(tokens: Token[]): PossibleInlineChildren[] {
    return parseInlineTokens(tokens, this);
  }

  extractText(tokens: Token[]): string {
    return tokens
      .filter((t) => t.type === "text" || t.type === "code_inline")
      .map((t) => t.content || "")
      .join("");
  }
}
