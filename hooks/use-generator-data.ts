"use client"

import { GeneratorData } from "@/types/generator"
import { useState, useCallback } from "react"

const STORAGE_KEY = "generator_data"

/** Track last picked value per pool to avoid immediate repeats */
const lastPicked = new Map<string, string>()

function pickRandom<T>(arr: T[], poolKey?: string): T {
  if (arr.length === 0) return undefined as T
  if (arr.length === 1) return arr[0]
  
  // Try to pick a different value than last time
  const maxAttempts = 5
  for (let i = 0; i < maxAttempts; i++) {
    const idx = Math.floor(Math.random() * arr.length)
    const value = arr[idx]
    const key = poolKey ? `${poolKey}:${String(value)}` : String(value)
    
    if (poolKey && lastPicked.get(poolKey) === value) {
      continue // Skip if same as last picked
    }
    
    // Store this as last picked
    if (poolKey) {
      lastPicked.set(poolKey, value as string)
    }
    return value
  }
  
  // If all attempts failed (very small pool), just return random
  const idx = Math.floor(Math.random() * arr.length)
  if (poolKey) {
    lastPicked.set(poolKey, arr[idx] as string)
  }
  return arr[idx]
}

export function useGeneratorData() {
  const [data, setData] = useState<GeneratorData | null>(() => {
    if (typeof globalThis.window === "undefined") return null
    try {
      const stored = localStorage.getItem(STORAGE_KEY)
      return stored ? JSON.parse(stored) : null
    } catch {
      return null
    }
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchData = useCallback(async (forceRefresh = false) => {
    setLoading(true)
    setError(null)
    try {
      const res = await fetch("/mock")
      if (!res.ok) throw new Error("Failed to fetch generator data")
      const json: GeneratorData = await res.json()
      setData(json)
      try {
        // If force refresh, remove old data first
        if (forceRefresh) {
          localStorage.removeItem(STORAGE_KEY)
          // Clear the pick history to get fresh random selection
          lastPicked.clear()
        }
        localStorage.setItem(STORAGE_KEY, JSON.stringify(json))
      } catch {
        // quota exceeded — ignore
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error")
    } finally {
      setLoading(false)
    }
  }, [])

  /** Ensure data is loaded — fetch only if not already in state/storage */
  const ensureData = useCallback(async (): Promise<GeneratorData | null> => {
    if (data) return data
    await fetchData()
    // After fetch, data state may not have updated yet; re-read localStorage
    try {
      const stored = localStorage.getItem(STORAGE_KEY)
      return stored ? JSON.parse(stored) : null
    } catch {
      return null
    }
  }, [data, fetchData])

  /**
   * Sample a random value for a given token path.
   * token format: "category.subType.modifier" or "category.subType.modifier.attribute"
   * e.g. "name.first.elf" or "name.first.elf.male"
   */
  const sampleToken = useCallback(
    (
      generatorData: GeneratorData,
      category: string,
      subType: string,
      modifier: string,
      attribute?: string
    ): string | null => {
      const cat = generatorData[category]
      if (!cat) return null
      const sub = cat.subTypes[subType]
      if (!sub) return null
      const mod = sub.pools[modifier]
      if (!mod) return null
      
      // Check if modifier has attributes (union type discrimination)
      if ('attributes' in mod && mod.attributes) {
        // If attribute is provided (not undefined/empty), use attribute pool
        if (attribute && mod.attributes[attribute]) {
          const pool = mod.attributes[attribute]
          if (!pool || pool.values.length === 0) return null
          const poolKey = `${category}.${subType}.${modifier}.${attribute}`
          return pickRandom(pool.values, poolKey)
        }
        // If no attribute provided but modifier has attributes, return null
        return null
      }

      // Modifier has direct values (no attributes)
      if ('values' in mod && mod.values && mod.values.length > 0) {
        const poolKey = `${category}.${subType}.${modifier}`
        return pickRandom(mod.values, poolKey)
      }
      
      return null
    },
    []
  )

  return { data, loading, error, fetchData, ensureData, sampleToken }
}
