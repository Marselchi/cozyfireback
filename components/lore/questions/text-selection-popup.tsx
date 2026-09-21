"use client"

import { useEffect, useState, useCallback, useRef } from "react"
import { Quote } from "lucide-react"

interface TextSelectionPopupProps {
  /** The container element to listen for selections in */
  containerRef: React.RefObject<HTMLElement | null>
  /** Whether the popup is enabled (question mode) */
  enabled: boolean
}

interface PopupPosition {
  x: number
  y: number
}

export function TextSelectionPopup({
  containerRef,
  enabled,
}: Readonly<TextSelectionPopupProps>) {
  const [position, setPosition] = useState<PopupPosition | null>(null)
  const [selectedText, setSelectedText] = useState("")
  const popupRef = useRef<HTMLDivElement>(null)

  const handleMouseUp = useCallback(() => {
    if (!enabled) return

    // Small delay to let the selection finalize
    setTimeout(() => {
      const selection = globalThis.getSelection()
      if (!selection || selection.isCollapsed || !selection.rangeCount) {
        setPosition(null)
        setSelectedText("")
        return
      }

      const text = selection.toString().trim()
      if (!text) {
        setPosition(null)
        setSelectedText("")
        return
      }

      // Check that the selection is within our container
      const range = selection.getRangeAt(0)
      if (
        containerRef.current &&
        !containerRef.current.contains(range.commonAncestorContainer)
      ) {
        setPosition(null)
        setSelectedText("")
        return
      }

      const rect = range.getBoundingClientRect()
      const containerRect = containerRef.current?.getBoundingClientRect()

      if (containerRect) {
        setPosition({
          x: rect.left - containerRect.left + rect.width / 2,
          y: rect.top - containerRect.top,
        })
        setSelectedText(text)
      }
    }, 10)
  }, [enabled, containerRef])

  const handleMouseDown = useCallback(
    (e: MouseEvent) => {
      // If clicking outside the popup, hide it
      if (popupRef.current && !popupRef.current.contains(e.target as Node)) {
        setPosition(null)
        setSelectedText("")
      }
    },
    []
  )

  useEffect(() => {
    const container = containerRef.current
    if (!container || !enabled) return

    container.addEventListener("mouseup", handleMouseUp)
    document.addEventListener("mousedown", handleMouseDown)

    return () => {
      container.removeEventListener("mouseup", handleMouseUp)
      document.removeEventListener("mousedown", handleMouseDown)
    }
  }, [containerRef, enabled, handleMouseUp, handleMouseDown])

  const handleCopyQuote = useCallback(() => {
    if (!selectedText) return

    const quotedText = `<? ${selectedText}`
    navigator.clipboard.writeText(quotedText).then(() => {
      setPosition(null)
      setSelectedText("")
      globalThis.getSelection()?.removeAllRanges()
    })
  }, [selectedText])

  if (!position || !enabled) return null

  return (
    <div
      ref={popupRef}
      className="absolute z-50 animate-in fade-in-0 zoom-in-95 duration-150"
      style={{
        left: `${position.x}px`,
        top: `${position.y}px`,
        transform: "translate(-50%, -100%) translateY(-8px)",
      }}
    >
      <button
        onClick={handleCopyQuote}
        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-foreground text-background text-xs font-medium shadow-lg hover:opacity-90 transition-opacity"
      >
        <Quote className="w-3 h-3" />
        Скопировать цитату
      </button>
      {/* Arrow */}
      <div
        className="absolute left-1/2 -translate-x-1/2 -bottom-1 w-2 h-2 rotate-45 bg-foreground"
      />
    </div>
  )
}
