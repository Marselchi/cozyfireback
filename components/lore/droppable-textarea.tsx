"use client"

import type React from "react"

import { useRef, useCallback, useState } from "react"
import { Textarea } from "@/components/ui/textarea"
import { cn } from "@/lib/utils"

interface DroppableTextareaProps {
  value: string
  onChange: (value: string) => void
  onSpoilerDrop: (spoilerId: string, position: number) => void
  placeholder?: string
  readOnly?: boolean
  className?: string
  style?: React.CSSProperties
}

export function DroppableTextarea({
  value,
  onChange,
  onSpoilerDrop,
  placeholder,
  readOnly = false,
  className,
  style,
}: DroppableTextareaProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const [isDragOver, setIsDragOver] = useState(false)
  const [dropPosition, setDropPosition] = useState<number | null>(null)

  const calculateDropPosition = useCallback(
    (e: React.DragEvent) => {
      if (!textareaRef.current) return 0

      const textarea = textareaRef.current
      const rect = textarea.getBoundingClientRect()
      const x = e.clientX - rect.left - Number.parseInt(getComputedStyle(textarea).paddingLeft)
      const y = e.clientY - rect.top - Number.parseInt(getComputedStyle(textarea).paddingTop)

      // Create a temporary canvas to measure text accurately
      const canvas = document.createElement("canvas")
      const ctx = canvas.getContext("2d")
      if (ctx) {
        const style = getComputedStyle(textarea)
        ctx.font = `${style.fontSize} ${style.fontFamily}`

        const lines = value.split("\n")
        const lineHeight = Number.parseInt(style.lineHeight) || Number.parseInt(style.fontSize) * 1.2
        const lineIndex = Math.max(0, Math.min(Math.floor(y / lineHeight), lines.length - 1))

        if (lines[lineIndex]) {
          // Measure character positions in the line
          const line = lines[lineIndex]
          let charIndex = 0
          let currentWidth = 0

          for (let i = 0; i < line.length; i++) {
            const charWidth = ctx.measureText(line[i]).width
            if (currentWidth + charWidth / 2 > x) {
              charIndex = i
              break
            }
            currentWidth += charWidth
            charIndex = i + 1
          }

          // Calculate absolute position
          let position = 0
          for (let i = 0; i < lineIndex; i++) {
            position += lines[i].length + 1 // +1 for newline
          }
          position += Math.max(0, Math.min(charIndex, line.length))

          return Math.max(0, Math.min(position, value.length))
        }
      }

      // Fallback to approximate calculation
      const style = getComputedStyle(textarea)
      const lineHeight = Number.parseInt(style.lineHeight) || 20
      const fontSize = Number.parseInt(style.fontSize) || 14
      const charWidth = fontSize * 0.6

      const lines = value.split("\n")
      const lineIndex = Math.max(0, Math.min(Math.floor(y / lineHeight), lines.length - 1))
      const charIndex = Math.max(0, Math.min(Math.floor(x / charWidth), lines[lineIndex]?.length || 0))

      let position = 0
      for (let i = 0; i < lineIndex; i++) {
        position += lines[i].length + 1
      }
      position += charIndex

      return Math.max(0, Math.min(position, value.length))
    },
    [value],
  )

  const handleDragOver = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      e.dataTransfer.dropEffect = "move"

      if (readOnly) {
        e.dataTransfer.dropEffect = "none"
        return
      }

      setIsDragOver(true)
      const position = calculateDropPosition(e)
      setDropPosition(position)
    },
    [readOnly, calculateDropPosition],
  )

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    const rect = textareaRef.current?.getBoundingClientRect()
    if (rect) {
      const x = e.clientX
      const y = e.clientY
      const isOutside = x < rect.left || x > rect.right || y < rect.top || y > rect.bottom

      if (isOutside) {
        setIsDragOver(false)
        setDropPosition(null)
      }
    }
  }, [])

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      setIsDragOver(false)

      if (readOnly) {
        setDropPosition(null)
        return
      }

      const spoilerId = e.dataTransfer.getData("text/plain")
      if (spoilerId && dropPosition !== null) {
        onSpoilerDrop(spoilerId, dropPosition)
      }
      setDropPosition(null)
    },
    [onSpoilerDrop, dropPosition, readOnly],
  )

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      onChange(e.target.value)
    },
    [onChange],
  )

  const getDropIndicatorStyle = useCallback(() => {
    if (!isDragOver || readOnly || dropPosition === null || !textareaRef.current) {
      return { display: "none" }
    }

    const textarea = textareaRef.current
    const style = getComputedStyle(textarea)
    const lineHeight = Number.parseInt(style.lineHeight) || Number.parseInt(style.fontSize) * 1.2
    const paddingLeft = Number.parseInt(style.paddingLeft)
    const paddingTop = Number.parseInt(style.paddingTop)

    // Calculate line and character position
    const textBeforePosition = value.slice(0, dropPosition)
    const lines = textBeforePosition.split("\n")
    const lineIndex = lines.length - 1
    const charInLine = lines[lineIndex].length

    // Create temporary canvas for accurate width measurement
    const canvas = document.createElement("canvas")
    const ctx = canvas.getContext("2d")
    let leftOffset = paddingLeft

    if (ctx) {
      ctx.font = `${style.fontSize} ${style.fontFamily}`
      leftOffset += ctx.measureText(lines[lineIndex]).width
    } else {
      // Fallback
      const fontSize = Number.parseInt(style.fontSize) || 14
      leftOffset += charInLine * fontSize * 0.6
    }

    return {
      left: `${leftOffset}px`,
      top: `${paddingTop + lineIndex * lineHeight}px`,
      height: `${lineHeight}px`,
    }
  }, [isDragOver, readOnly, dropPosition, value])

  return (
    <div className="h-full">
      <Textarea
        ref={textareaRef}
        value={value}
        onChange={handleChange}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        placeholder={placeholder}
        readOnly={readOnly}
        className={cn(
          "transition-all duration-200",
          isDragOver && !readOnly && "ring-2 ring-primary ring-offset-2 bg-primary/5",
          className,
        )}
        style={style}
      />
      {isDragOver && !readOnly && dropPosition !== null && (
        <div
          className="absolute w-0.5 bg-primary animate-pulse pointer-events-none z-10"
          style={getDropIndicatorStyle()}
        />
      )}
    </div>
  )
}
