"use client"

import { useEffect, useRef } from "react"

export function useInfiniteScrollSentinel({
  onIntersect,
  enabled,
  rootRef,
}: {
  onIntersect: () => void
  enabled: boolean
  rootRef?: React.RefObject<HTMLElement | null>
}) {
  const sentinelRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (!enabled) return
    const sentinel = sentinelRef.current
    if (!sentinel) return

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          onIntersect()
        }
      },
      { root: rootRef?.current ?? null, rootMargin: "80px" },
    )

    observer.observe(sentinel)
    return () => observer.disconnect()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, onIntersect])

  return sentinelRef
}
