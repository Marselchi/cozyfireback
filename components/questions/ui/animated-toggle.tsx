"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"

interface AnimatedToggleProps {
  enabled: boolean
  onToggle: (enabled: boolean) => void
  label: string
}

export function AnimatedToggle({ enabled, onToggle, label }: AnimatedToggleProps) {
  const [isPulsing, setIsPulsing] = useState(false)

  const handleToggle = () => {
    setIsPulsing(true)
    onToggle(!enabled)
    setTimeout(() => setIsPulsing(false), 300)
  }

  return (
    <Button
      variant={enabled ? "default" : "outline"}
      size="sm"
      onClick={handleToggle}
      className={`transition-all duration-200 border-0 ${
        isPulsing ? "animate-pulse scale-105" : ""
      } ${enabled ? "dark:bg-blue-700 bg-blue-300 dark:hover:bg-blue-800 hover:bg-blue-400 text-primary" : "dark:hover:bg-blue-700 hover:bg-blue-300"}`}
    >
      {label}
    </Button>
  )
}
