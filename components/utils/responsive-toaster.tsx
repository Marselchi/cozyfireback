"use client"

import { Toaster } from "@/components/ui/sonner"
import { useMediaQuery } from "@/hooks/use-media-query"

export function ResponsiveToaster() {
  const isMobile = useMediaQuery("(max-width: 767px)")

  return (
    <Toaster position={isMobile ? "top-center" : "bottom-left"} />
  )
}