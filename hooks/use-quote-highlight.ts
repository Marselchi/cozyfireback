"use client"

import { useCallback, useEffect, useRef } from "react"

/**
 * Hook to handle scrolling to and highlighting quoted text in the lore content.
 * Searches for the quoted text inside the container, scrolls to it, and highlights it.
 */
export function useQuoteHighlight(containerRef: React.RefObject<HTMLElement | null>) {
  const highlightTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => () => {
    if (highlightTimeoutRef.current) clearTimeout(highlightTimeoutRef.current)
  }, [])

  const scrollToQuote = useCallback((quoteText: string) => {
    const container = containerRef.current
    if (!container || !quoteText) return

    // Очистка предыдущего highlighting
    container.querySelectorAll(".quote-highlight").forEach((el) => {
      const parent = el.parentNode
      if (parent) {
        parent.replaceChild(document.createTextNode(el.textContent || ""), el)
        parent.normalize()
      }
    })

    if (highlightTimeoutRef.current) clearTimeout(highlightTimeoutRef.current)

    const treeWalker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT)
    const normalize = (t: string) => t.toLowerCase().replace(/\s+/g, ' ').trim()
    const target = normalize(quoteText)
    
    let foundNode: Text | null = null
    let startOffset = 0
    let endOffset = 0

    // Вспомогательная функция для поиска позиции в исходном тексте по нормализованной позиции
    const findOffsets = (original: string, normalized: string, normStart: number, normLength: number) => {
      // Сопоставляем символы между нормализованным и оригинальным текстом
      let origIdx = 0
      let normIdx = 0
      let realStart = -1
      let realEnd = -1

      // Пропускаем символы до начала совпадения в нормализованном тексте
      while (normIdx < normStart && origIdx < original.length) {
        if (/\s/.test(normalized[normIdx])) {
          normIdx++
          continue
        }
        if (/\s/.test(original[origIdx])) {
          origIdx++
          continue
        }
        if (original[origIdx].toLowerCase() === normalized[normIdx]) {
          normIdx++
          origIdx++
        } else {
          origIdx++
        }
      }
      realStart = origIdx

      // Находим конец совпадения
      const normEnd = normStart + normLength
      while (normIdx < normEnd && origIdx < original.length) {
        if (/\s/.test(normalized[normIdx])) {
          normIdx++
          continue
        }
        if (/\s/.test(original[origIdx])) {
          origIdx++
          continue
        }
        if (original[origIdx].toLowerCase() === normalized[normIdx]) {
          normIdx++
          origIdx++
        } else {
          origIdx++
        }
      }
      realEnd = origIdx

      return { start: realStart, end: realEnd }
    }

    while (treeWalker.nextNode()) {
      const textNode = treeWalker.currentNode as Text
      const content = textNode.textContent || ""
      const normalizedContent = normalize(content)

      const idx = normalizedContent.indexOf(target)
      if (idx !== -1) {
        foundNode = textNode
        const offsets = findOffsets(content, normalizedContent, idx, target.length)
        startOffset = offsets.start
        endOffset = offsets.end
        break
      }
    }

    if (foundNode && startOffset !== -1 && endOffset !== -1) {
      const range = document.createRange()
      range.setStart(foundNode, startOffset)
      range.setEnd(foundNode, endOffset)

      const highlightSpan = document.createElement("span")
      highlightSpan.className = "quote-highlight"
      range.surroundContents(highlightSpan)

      highlightSpan.scrollIntoView({ behavior: "smooth", block: "center" })

      highlightTimeoutRef.current = setTimeout(() => {
        highlightSpan.classList.add("quote-highlight-fade")
        setTimeout(() => {
          const parent = highlightSpan.parentNode
          if (parent) {
            parent.replaceChild(document.createTextNode(highlightSpan.textContent || ""), highlightSpan)
            parent.normalize()
          }
        }, 500)
      }, 3000)
    }
  }, [containerRef])

  return { scrollToQuote }
}
