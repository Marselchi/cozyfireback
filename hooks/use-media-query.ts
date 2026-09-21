"use client"

import { useEffect, useState } from "react"

export function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState<boolean>(() => {
    // важно для SSR / hydration
    if (typeof globalThis.window === "undefined") return false
    return globalThis.matchMedia(query).matches
  })

  useEffect(() => {
    const media = globalThis.matchMedia(query)

    const listener = (e: MediaQueryListEvent) => {
      setMatches(e.matches)
    }

    media.addEventListener("change", listener)

    return () => {
      media.removeEventListener("change", listener)
    }
  }, [query])

  return matches
}