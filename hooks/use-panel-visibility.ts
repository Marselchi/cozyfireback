"use client"

import { useState, useEffect, useCallback } from "react"

interface PanelVisibility {
  preview: boolean
  metadata: boolean
  specialBlocks: boolean
}

const DEFAULT_VISIBILITY: PanelVisibility = {
  preview: true,
  metadata: true,
  specialBlocks: true,
}

export function usePanelVisibility(storageKey = "panel-visibility"): {
  visibility: PanelVisibility
  togglePreview: () => void
  toggleMetadata: () => void
  toggleSpecialBlocks: () => void
  showSpecialBlocks: () => void
  resetVisibility: () => void
} {
  const [visibility, setVisibility] = useState<PanelVisibility>(DEFAULT_VISIBILITY)
  const [initialized, setInitialized] = useState(false)

  // Load saved visibility from localStorage on mount
  useEffect(() => {
    try {
      const savedVisibility = localStorage.getItem(storageKey)
      if (savedVisibility) {
        setVisibility(JSON.parse(savedVisibility))
      }
      setInitialized(true)
    } catch (error) {
      console.error("Error loading panel visibility from localStorage:", error)
      setInitialized(true)
    }
  }, [storageKey])

  // Save visibility to localStorage whenever it changes
  useEffect(() => {
    if (!initialized) return

    try {
      localStorage.setItem(storageKey, JSON.stringify(visibility))
    } catch (error) {
      console.error("Error saving panel visibility to localStorage:", error)
    }
  }, [visibility, storageKey, initialized])

  const togglePreview = useCallback(() => {
    setVisibility((prev) => ({ ...prev, preview: !prev.preview }))
  }, [])

  const toggleMetadata = useCallback(() => {
    setVisibility((prev) => ({ ...prev, metadata: !prev.metadata }))
  }, [])

  const toggleSpecialBlocks = useCallback(() => {
    setVisibility((prev) => ({ ...prev, specialBlocks: !prev.specialBlocks }))
  }, [])

  const showSpecialBlocks = useCallback(() => {
    setVisibility((prev) => ({ ...prev, specialBlocks: true }))
  }, [])

  const resetVisibility = useCallback(() => {
    setVisibility(DEFAULT_VISIBILITY)
  }, [])

  return {
    visibility,
    togglePreview,
    toggleMetadata,
    toggleSpecialBlocks,
    showSpecialBlocks,
    resetVisibility,
  }
}
