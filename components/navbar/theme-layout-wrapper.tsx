"use client"

import type React from "react"

import { ThemeProvider } from "next-themes"
import { useEffect, useState } from "react"

interface ThemeLayoutWrapperProps {
  children: React.ReactNode
}

export function ThemeLayoutWrapper({ children }: ThemeLayoutWrapperProps) {
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  return (
    <ThemeProvider attribute="class" defaultTheme="dark" enableSystem disableTransitionOnChange={true}>
      <div className="relative min-h-screen">
        {children}
      </div>
    </ThemeProvider>
  )
}
