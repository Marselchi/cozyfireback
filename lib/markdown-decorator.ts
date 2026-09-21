import type { NodeEntry } from "slate"
import type { CustomRange } from "@/types/editor"

// This shows markdown syntax WITH visual preview styling
// The actual markdown characters remain visible and editable

type MarkdownToken = {
  start: number
  end: number
  contentStart: number
  contentEnd: number
  type:
    | "bold"
    | "italic"
    | "boldItalic"
    | "strikethrough"
    | "code"
    | "quote"
    | "heading"
    | "blockquote"
    | "list"
    | "numberedList"
    | "link"
    | "hr"
    | "placeholder"
}

function tokenizeMarkdown(text: string): MarkdownToken[] {
  const tokens: MarkdownToken[] = []

  const addToken = (
    start: number,
    end: number,
    contentStart: number,
    contentEnd: number,
    type: MarkdownToken["type"],
  ) => {
    // Проверка на пересечение
    const overlaps = tokens.some(
      (t) => (start >= t.start && start < t.end) || (end > t.start && end <= t.end) || (start < t.start && end > t.end),
    )
    if (!overlaps) {
      tokens.push({ start, end, contentStart, contentEnd, type })
    }
  }

  // Порядок важен: сначала длинные шаблоны
  const patterns = [
    { regex: /\{\{([^}]+?)\}\}/g, type: "placeholder" as const, marker: 2 },
    { regex: /\*\*\*([^*]+?)\*\*\*/g, type: "boldItalic" as const, marker: 3 },
    { regex: /___([^_]+?)___/g, type: "boldItalic" as const, marker: 3 },
    { regex: /\*\*([^*]+?)\*\*/g, type: "bold" as const, marker: 2 },
    { regex: /~~([^~]+?)~~/g, type: "strikethrough" as const, marker: 2 },
    { regex: /(```[\s\S]*?```)|(`[^`]+`)/g, type: "code" as const, marker: 1 },
    { regex: /<\?[\s\S]*?<\?/g, type: "quote" as const, marker: 1 },
    { regex: /_([^_]+?)_/g, type: "italic" as const, marker: 1 },
    { regex: /\*([^*]+?)\*/g, type: "italic" as const, marker: 1 },
  ]

  for (const { regex, type, marker } of patterns) {
    let match
    while ((match = regex.exec(text)) !== null) {
      const start = match.index
      const end = start + match[0].length
      const contentStart = start + marker
      const contentEnd = end - marker

      addToken(start, end, contentStart, contentEnd, type)
    }
  }

  // Одиночные проверки (только начало строки)
  const lines = text.split(/\n/)
  let offset = 0

  for (const line of lines) {
    const headingMatch = /^(#{1,6})\s(.*)$/.exec(line)
    if (headingMatch) {
      addToken(offset, offset + headingMatch[0].length, offset, offset + headingMatch[0].length, "heading")
    }

    const blockquoteMatch = /^>\s/.exec(line)
    if (blockquoteMatch) {
      addToken(offset, offset + blockquoteMatch[0].length, offset, offset + blockquoteMatch[0].length, "blockquote")
    }

    const numberedListMatch = /^\d+\.\s/.exec(line)
    if (numberedListMatch) {
      addToken(
        offset,
        offset + numberedListMatch[0].length,
        offset,
        offset + numberedListMatch[0].length,
        "numberedList",
      )
    }

    const listMatch = /^[-*+]\s/.exec(line)
    if (listMatch) {
      addToken(offset, offset + listMatch[0].length, offset, offset + listMatch[0].length, "list")
    }

    offset += line.length + 1 // +1 для \n
  }

  return tokens
}

export function decorateMarkdown([node, path]: NodeEntry): CustomRange[] {
  const ranges: CustomRange[] = []

  if (!node || typeof node !== "object" || !("text" in node)) {
    return ranges
  }

  const text = (node as { text: string }).text
  if (!text) return ranges

  const tokens = tokenizeMarkdown(text)

  for (const token of tokens) {
    // Style the ENTIRE token including markers - so syntax is visible but styled
    const range: CustomRange = {
      anchor: { path, offset: token.start },
      focus: { path, offset: token.end },
    }

    switch (token.type) {
      case "bold":
        range.bold = true
        break
      case "italic":
        range.italic = true
        break
      case "boldItalic":
        range.bold = true
        range.italic = true
        break
      case "strikethrough":
        range.strike = true
        break
      case "code":
        range.code = true
        break
      case "quote":
        range.quote = true
        break
      case "heading":
        range.heading = true
        break
      case "blockquote":
        range.blockquote = true
        break
      case "list":
      case "numberedList":
        range.list = true
        break
      case "placeholder":
        range.placeholder = true
        break
    }

    ranges.push(range)
  }

  return ranges
}
