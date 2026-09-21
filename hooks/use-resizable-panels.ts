"use client"

import { useState, useEffect, useCallback } from "react"

interface PanelSizes {
  leftPanelWidth: number
  specialBlocksPanelHeight: number
  metadataPanelHeight: number
}

const DEFAULT_SIZES: PanelSizes = {
  leftPanelWidth: 50, // 50% of container width
  specialBlocksPanelHeight: 200, // 200px height
  metadataPanelHeight: 250, // 250px height
}

const MIN_PANEL_WIDTH = 25 // Minimum 25% width
const MAX_PANEL_WIDTH = 75 // Maximum 75% width
const MIN_PANEL_HEIGHT = 100 // Minimum 100px height
const MAX_PANEL_HEIGHT = 500 // Maximum 500px height

export function useResizablePanels(storageKey = "editor-panel-sizes"): {
  sizes: PanelSizes
  updateLeftPanelWidth: (width: number) => void
  updateSpecialBlocksPanelHeight: (height: number) => void
  updateMetadataPanelHeight: (height: number) => void
  autoResizeForVisibility: (showPreview: boolean, showMetadata: boolean) => void
  resetSizes: () => void
} {
  const [sizes, setSizes] = useState<PanelSizes>(DEFAULT_SIZES)
  const [initialized, setInitialized] = useState(false)

  // Load saved sizes from localStorage on mount
  useEffect(() => {
    try {
      const savedSizes = localStorage.getItem(storageKey)
      if (savedSizes) {
        const parsed = JSON.parse(savedSizes)
        setSizes({
          leftPanelWidth: Math.max(
            MIN_PANEL_WIDTH,
            Math.min(MAX_PANEL_WIDTH, parsed.leftPanelWidth || DEFAULT_SIZES.leftPanelWidth),
          ),
          specialBlocksPanelHeight: Math.max(
            MIN_PANEL_HEIGHT,
            Math.min(MAX_PANEL_HEIGHT, parsed.specialBlocksPanelHeight || DEFAULT_SIZES.specialBlocksPanelHeight),
          ),
          metadataPanelHeight: Math.max(
            MIN_PANEL_HEIGHT,
            Math.min(MAX_PANEL_HEIGHT, parsed.metadataPanelHeight || DEFAULT_SIZES.metadataPanelHeight),
          ),
        })
      }
      setInitialized(true)
    } catch (error) {
      console.error("Error loading panel sizes from localStorage:", error)
      setInitialized(true)
    }
  }, [storageKey])

  // Save sizes to localStorage whenever they change
  useEffect(() => {
    if (!initialized) return

    try {
      localStorage.setItem(storageKey, JSON.stringify(sizes))
    } catch (error) {
      console.error("Error saving panel sizes to localStorage:", error)
    }
  }, [sizes, storageKey, initialized])

  const updateLeftPanelWidth = useCallback((width: number) => {
    const constrainedWidth = Math.max(MIN_PANEL_WIDTH, Math.min(MAX_PANEL_WIDTH, width))
    setSizes((prev) => ({ ...prev, leftPanelWidth: constrainedWidth }))
  }, [])

  const updateSpecialBlocksPanelHeight = useCallback((height: number) => {
    const constrainedHeight = Math.max(MIN_PANEL_HEIGHT, Math.min(MAX_PANEL_HEIGHT, height))
    setSizes((prev) => ({ ...prev, specialBlocksPanelHeight: constrainedHeight }))
  }, [])

  const updateMetadataPanelHeight = useCallback((height: number) => {
    const constrainedHeight = Math.max(MIN_PANEL_HEIGHT, Math.min(MAX_PANEL_HEIGHT, height))
    setSizes((prev) => ({ ...prev, metadataPanelHeight: constrainedHeight }))
  }, [])

  const autoResizeForVisibility = useCallback(
    (showPreview: boolean, showMetadata: boolean) => {
      // Auto-adjust left panel width based on right panel visibility
      if (!showPreview && !showMetadata) {
        // No right panel, left panel can be full width
        setSizes((prev) => ({ ...prev, leftPanelWidth: 100 }))
      } else if (sizes.leftPanelWidth === 100) {
        // Right panel is being shown, restore reasonable left panel width
        setSizes((prev) => ({ ...prev, leftPanelWidth: DEFAULT_SIZES.leftPanelWidth }))
      }
    },
    [sizes.leftPanelWidth],
  )

  const resetSizes = useCallback(() => {
    setSizes(DEFAULT_SIZES)
  }, [])

  return {
    sizes,
    updateLeftPanelWidth,
    updateSpecialBlocksPanelHeight,
    updateMetadataPanelHeight,
    autoResizeForVisibility,
    resetSizes,
  }
}
