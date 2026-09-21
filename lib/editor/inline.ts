import type {
  Token,
  PossibleInlineChildren,
  ParserContext,
} from "@/types/node-definition";
import type { CustomText, ImageElement, DiceElement } from "@/types/editor";

type InlineStackItem =
  | CustomText
  | { type: "link"; url: string; children: CustomText[]; title?: string };

export function parseInlineTokens(
  tokens: Token[],
  _context: ParserContext,
): PossibleInlineChildren[] {
  const result: PossibleInlineChildren[] = [];
  const stack: InlineStackItem[] = [{ text: "" }];

  for (const token of tokens) {
    const current = stack.at(-1);

    switch (token.type) {
      case "text":
        if (current && "type" in current && current.type === "link") {
          const lastChild = current.children.at(-1);
          if (lastChild && !lastChild.bold && !lastChild.italic) {
            lastChild.text += token.content;
          } else {
            current.children.push({ text: token.content });
          }
        } else if (current && "text" in current && !("type" in current)) {
          // `current` is always the accumulator meant to receive this text —
          // either the base unmarked node, or the node a *_open handler just
          // pushed for the mark we're currently inside. Appending here
          // (instead of pushing a fresh unmarked node) is what keeps the
          // mark flags attached to the text that belongs to them.
          current.text += token.content;
        } else {
          stack.push({ text: token.content });
        }
        break;

      case "strong_open": {
        const inherited = finalizeCurrentStack(stack, result);
        stack.push({ text: "", ...inherited, bold: true });
        break;
      }
      case "strong_close":
        finalizeStack(stack, result);
        break;

      case "em_open": {
        const inherited = finalizeCurrentStack(stack, result);
        stack.push({ text: "", ...inherited, italic: true });
        break;
      }
      case "em_close":
        finalizeStack(stack, result);
        break;

      case "s_open": {
        const inherited = finalizeCurrentStack(stack, result);
        stack.push({ text: "", ...inherited, strikethrough: true });
        break;
      }
      case "s_close":
        finalizeStack(stack, result);
        break;

      case "u_open": {
        const inherited = finalizeCurrentStack(stack, result);
        stack.push({ text: "", ...inherited, underline: true });
        break;
      }
      case "u_close":
        finalizeStack(stack, result);
        break;

      case "code_inline": {
        const flags = activeFlags(stack);
        finalizeCurrentStack(stack, result);
        if (current && "type" in current && current.type === "link") {
          current.children.push({ text: token.content, code: true });
        } else {
          result.push({ text: token.content, ...flags, code: true });
          resumeIfMarked(stack, flags);
        }
        break;
      }

      case "link_open": {
        finalizeCurrentStack(stack, result);
        const href = token.attrGet("href") || "";
        const title = token.attrGet("title") || undefined;
        stack.push({ type: "link", url: href, children: [], title });
        break;
      }
      case "link_close": {
        const link = stack.pop() as {
          type: "link";
          url: string;
          children: CustomText[];
          title?: string;
        };
        if (link.children.length === 0) {
          link.children.push({ text: link.url });
        }
        result.push({ type: "link", url: link.url, children: link.children });
        break;
      }

      case "image": {
        finalizeCurrentStack(stack, result);
        const image: ImageElement = {
          type: "image",
          src: token.attrGet("src") || "",
          alt: token.content || "",
          title: token.attrGet("title") || undefined,
          children: [{ text: "" }],
        };
        result.push(image);
        break;
      }

      case "dice": {
        finalizeCurrentStack(stack, result);
        // Dice formulas aren't supported as link text — if we're mid-link,
        // just drop it rather than corrupting the link's children shape.
        if (!(current && "type" in current && current.type === "link")) {
          const meta = token.meta as {
            count?: number;
            sides?: number;
            modifier?: number;
            letter?: string;
          } | null;
          const dice: DiceElement = {
            type: "dice",
            id: `dice-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`,
            count: meta?.count ?? 1,
            sides: meta?.sides ?? 6,
            modifier: meta?.modifier ?? 0,
            letter: meta?.letter === "к" ? "к" : "d",
            children: [{ text: "" }],
          };
          result.push(dice);
        }
        break;
      }

      case "placeholder": {
        const flags = activeFlags(stack);
        finalizeCurrentStack(stack, result);
        if (current && "type" in current && current.type === "link") {
          current.children.push({
            text: `{{${token.content}}}`,
            placeholder: true,
          });
        } else {
          result.push({
            text: `{{${token.content}}}`,
            ...flags,
            placeholder: true,
          });
          resumeIfMarked(stack, flags);
        }
        break;
      }
      case "mark_open": {
        const inherited = finalizeCurrentStack(stack, result);
        stack.push({ text: "", ...inherited, mark: true });
        break;
      }
      case "mark_close":
        finalizeStack(stack, result);
        break;
      case "softbreak":
        finalizeCurrentStack(stack, result);
        result.push({ text: "", softbreak: true } as any);
        break;
      case "hardbreak":
        finalizeCurrentStack(stack, result);
        result.push({ text: "  \n", hardbreak: true } as any);
        break;

      default:
        break;
    }
  }

  // Flush remaining stack
  while (stack.length > 0) {
    const node = stack.pop();
    if (!node) break;
    if ("type" in node && node.type === "link") {
      console.warn("[editor] Unclosed link in inline stack");
    } else if ("text" in node && node.text) {
      result.push(node);
    }
  }

  return result.length > 0 ? result : [{ text: "" }];
}

function finalizeCurrentStack(
  stack: InlineStackItem[],
  result: PossibleInlineChildren[],
): Partial<CustomText> {
  const node = stack.at(-1);
  if (!node) return {};
  if ("text" in node && !("type" in node)) {
    stack.pop();
    if (node.text) {
      result.push(node);
      return {};
    }
    // Empty accumulator being discarded (e.g. strong_open immediately
    // followed by em_open, with no text token between them). Hand its
    // flags back instead of dropping them, so the caller can merge them
    // into the new node it's about to push — this is what makes nested
    // marks like ***bold italic*** (strong_open -> em_open -> text ->
    // em_close -> strong_close) keep both flags.
    const { text, ...flags } = node;
    return flags;
  }
  return {};
}

// Peek the flags of whatever mark accumulator is currently open, without
// consuming it. Used by isolated tokens (code_inline, placeholder) that
// need to inherit the enclosing mark(s) — e.g. `code` inside **bold**
// should render as bold *and* code, not lose the bold.
function activeFlags(stack: InlineStackItem[]): Partial<CustomText> {
  const node = stack.at(-1);
  if (node && "text" in node && !("type" in node)) {
    const { text, ...flags } = node;
    return flags;
  }
  return {};
}

// After inserting an isolated token (code_inline, placeholder) that broke
// the accumulator via finalizeCurrentStack, push a fresh empty accumulator
// carrying the same flags back onto the stack — so text after the isolated
// token keeps accumulating under the still-open mark(s) until the real
// *_close token arrives, instead of falling back to unmarked text.
function resumeIfMarked(
  stack: InlineStackItem[],
  flags: Partial<CustomText>,
): void {
  if (Object.keys(flags).length > 0) {
    stack.push({ text: "", ...flags });
  }
}

function finalizeStack(
  stack: InlineStackItem[],
  result: PossibleInlineChildren[],
): void {
  const node = stack.pop();
  if (!node) return;
  if ("type" in node && node.type === "link") {
    console.warn("[editor] Unclosed link finalizeStack");
  } else if ("text" in node && node.text) {
    result.push(node);
  }
}
