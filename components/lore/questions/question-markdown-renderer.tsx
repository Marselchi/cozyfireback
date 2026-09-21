"use client"

import { cn } from "@/lib/utils"
import MarkdownIt from "markdown-it"
import { useMemo } from "react"

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
})


function processQuotes(html: string): string {
  // Останавливаемся на следующем HTML-теге или переводе строки
  return html.replaceAll(/&lt;\?([^<\n]+)/g, (_, quote) => {
    const text = quote.trim()
    return `<span class="lore-quote" data-quote-text="${encodeURIComponent(text)}">${text}</span>`
  })
}
interface MarkdownRendererProps {
  content: string
  className?: string
  onQuoteClick?: (quoteText: string) => void
}

export function MarkdownRenderer({
  content,
  className,
  onQuoteClick,
}: Readonly<MarkdownRendererProps>) {
  const processedHtml = useMemo(() => {
    return processQuotes(md.render(content))
  }, [content])

  const handleClick = (e: React.MouseEvent) => {
    const target = e.target as HTMLElement
    const quoteEl = target.closest(".lore-quote") as HTMLElement
    if (quoteEl && onQuoteClick) {
      const quoteText = decodeURIComponent(
        quoteEl.dataset.quoteText || ""
      )
      if (quoteText) {
        onQuoteClick(quoteText)
      }
    }
  }

  return (
    <div
      className={cn(className, "markdown")}
      onClick={handleClick}
      dangerouslySetInnerHTML={{ __html: processedHtml }}
    />
  )
}