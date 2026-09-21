"use client"

import { useState, useEffect, useCallback } from "react"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { RefreshCw, Loader2, Copy, Check, AlertCircle } from "lucide-react"
import type { GeneratorData } from "@/types/generator"
import { useGeneratorData } from "@/hooks/use-generator-data"

interface AutoGeneratorDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Called when user confirms — receives the generated text to insert */
  onInsert: (text: string) => void
}

export function AutoGeneratorDialog({
  open,
  onOpenChange,
  onInsert,
}: AutoGeneratorDialogProps) {
  const { data, loading, error, fetchData, ensureData, sampleToken } =
    useGeneratorData()

  const [localData, setLocalData] = useState<GeneratorData | null>(null)

  const [category, setCategory] = useState<string>("")
  const [subType, setSubType] = useState<string>("")
  const [modifier, setModifier] = useState<string>("")
  const [attribute, setAttribute] = useState<string>("")

  const [result, setResult] = useState<string>("")
  const [copied, setCopied] = useState(false)

  // Load data when dialog opens
  useEffect(() => {
    if (!open) return
    ensureData().then((d) => {
      if (d) {
        setLocalData(d)
        // Pick first category/subType/modifier/attribute as default
        const cats = Object.keys(d)
        if (cats.length > 0 && !category) {
          const firstCat = cats[0]
          setCategory(firstCat)
          const subs = Object.keys(d[firstCat].subTypes)
          if (subs.length > 0) {
            const firstSub = subs[0]
            setSubType(firstSub)
            const mods = Object.keys(d[firstCat].subTypes[firstSub].pools)
            if (mods.length > 0) {
              const firstMod = mods[0]
              setModifier(firstMod)
              // Check if first modifier has attributes
              const firstModData = d[firstCat].subTypes[firstSub].pools[firstMod]
              const attrs = 'attributes' in firstModData && firstModData.attributes
                ? Object.keys(firstModData.attributes)
                : []
              if (attrs.length > 0) {
                setAttribute(attrs[0])
              } else {
                setAttribute("")
              }
            }
          }
        }
      }
    })
  }, [open]) // eslint-disable-line react-hooks/exhaustive-deps

  // Keep localData in sync when data changes (after refetch)
  useEffect(() => {
    if (data) setLocalData(data)
  }, [data])

  // Reset sub-selections when category changes
  const handleCategoryChange = useCallback(
    (val: string) => {
      setCategory(val)
      setSubType("")
      setModifier("")
      setAttribute("")
      setResult("")
      if (!localData) return
      const subs = Object.keys(localData[val]?.subTypes ?? {})
      if (subs.length > 0) {
        const firstSub = subs[0]
        setSubType(firstSub)
        const mods = Object.keys(localData[val].subTypes[firstSub]?.pools ?? {})
        if (mods.length > 0) {
          const firstMod = mods[0]
          setModifier(firstMod)
          // Check if first modifier has attributes
          const firstModData = localData[val].subTypes[firstSub].pools[firstMod]
          const attrs = 'attributes' in firstModData && firstModData.attributes
            ? Object.keys(firstModData.attributes)
            : []
          if (attrs.length > 0) {
            setAttribute(attrs[0])
          } else {
            setAttribute("")
          }
        }
      }
    },
    [localData]
  )

  const handleSubTypeChange = useCallback(
    (val: string) => {
      setSubType(val)
      setModifier("")
      setAttribute("")
      setResult("")
      if (!localData || !category) return
      const mods = Object.keys(
        localData[category]?.subTypes[val]?.pools ?? {}
      )
      if (mods.length > 0) {
        const firstMod = mods[0]
        setModifier(firstMod)
        // Check if first modifier has attributes
        const firstModData = localData[category]?.subTypes[val]?.pools[firstMod]
        const attrs = firstModData && 'attributes' in firstModData && firstModData.attributes
          ? Object.keys(firstModData.attributes)
          : []
        if (attrs.length > 0) {
          setAttribute(attrs[0])
        } else {
          setAttribute("")
        }
      }
    },
    [localData, category]
  )

  const handleModifierChange = useCallback(
    (val: string) => {
      setModifier(val)
      setAttribute("")
      setResult("")
      // Check if this modifier has attributes
      if (!localData || !category || !subType) return
      const modData = localData[category]?.subTypes[subType]?.pools[val]
      const attrs = modData && 'attributes' in modData && modData.attributes
        ? Object.keys(modData.attributes)
        : []
      if (attrs.length > 0) {
        setAttribute(attrs[0])
      } else {
        setAttribute("")
      }
    },
    [localData, category, subType]
  )

  const handleAttributeChange = useCallback((val: string) => {
    setAttribute(val)
    setResult("")
  }, [])

  const generate = useCallback(() => {
    if (!localData || !category || !subType || !modifier) return
    const value = sampleToken(localData, category, subType, modifier, attribute || undefined)
    setResult(value ?? "Результатов не найдено")
  }, [localData, category, subType, modifier, attribute, sampleToken])

  const handleCopy = useCallback(() => {
    if (!result) return
    navigator.clipboard.writeText(result).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    })
  }, [result])

  const handleInsert = useCallback(() => {
    if (!result) return
    onInsert(result)
    onOpenChange(false)
  }, [result, onInsert, onOpenChange])

  const handleRefetch = useCallback(() => {
    fetchData(true)
  }, [fetchData])

  const categoryList = localData ? Object.keys(localData) : []
  const subTypeList =
    localData && category ? Object.keys(localData[category]?.subTypes ?? {}) : []
  const modifierList =
    localData && category && subType
      ? Object.keys(localData[category]?.subTypes[subType]?.pools ?? {})
      : []
  const attributeList =
    localData && category && subType && modifier
      ? (() => {
          const modData = localData[category]?.subTypes[subType]?.pools[modifier]
          return (modData && 'attributes' in modData && modData.attributes)
            ? Object.keys(modData.attributes)
            : []
        })()
      : []

  const hasAttributes = attributeList.length > 0
  const canGenerate = !!category && !!subType && !!modifier && !!localData

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center justify-between gap-2">
            <span>Сгенерировать текст</span>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleRefetch}
              disabled={loading}
              className="h-7 gap-1.5 text-xs text-muted-foreground"
            >
              {loading ? (
                <Loader2 className="h-3 w-3 animate-spin" />
              ) : (
                <RefreshCw className="h-3 w-3" />
              )}
              Обновить списки
            </Button>
          </DialogTitle>
        </DialogHeader>

        {error && (
          <div className="flex items-center gap-2 rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">
            <AlertCircle className="h-4 w-4 shrink-0" />
            {error}
          </div>
        )}

        <div className="space-y-4 py-2">
          {/* Category */}
          <div className="space-y-1.5">
            <Label htmlFor="ag-category">Категория</Label>
            <Select value={category} onValueChange={handleCategoryChange}>
              <SelectTrigger id="ag-category" className="w-full">
                <SelectValue placeholder="Выберите категорию…" />
              </SelectTrigger>
              <SelectContent>
                {categoryList.map((key) => (
                  <SelectItem key={key} value={key}>
                    {localData![key].label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Sub-type */}
          <div className="space-y-1.5">
            <Label htmlFor="ag-subtype">Подтип</Label>
            <Select
              value={subType}
              onValueChange={handleSubTypeChange}
              disabled={!category}
            >
              <SelectTrigger id="ag-subtype" className="w-full">
                <SelectValue placeholder="Выберите подтип…" />
              </SelectTrigger>
              <SelectContent>
                {subTypeList.map((key) => (
                  <SelectItem key={key} value={key}>
                    {localData![category]?.subTypes[key]?.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Modifier */}
          <div className="space-y-1.5">
            <Label htmlFor="ag-modifier">Модификатор</Label>
            <Select
              value={modifier}
              onValueChange={handleModifierChange}
              disabled={!subType}
            >
              <SelectTrigger id="ag-modifier" className="w-full">
                <SelectValue placeholder="Выберите модификатор…" />
              </SelectTrigger>
              <SelectContent>
                {modifierList.map((key) => (
                  <SelectItem key={key} value={key}>
                    {localData![category]?.subTypes[subType]?.pools[key]?.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Attribute (optional) */}
          {hasAttributes && (
            <div className="space-y-1.5">
              <Label htmlFor="ag-attribute">Атрибут (опционально)</Label>
              <Select
                value={attribute}
                onValueChange={handleAttributeChange}
                disabled={!modifier}
              >
                <SelectTrigger id="ag-attribute" className="w-full">
                  <SelectValue placeholder="Выберите атрибут…" />
                </SelectTrigger>
                <SelectContent>
                  {attributeList.map((key) => {
                    const modData = localData![category]?.subTypes[subType]?.pools[modifier]
                    const attrPool = modData && 'attributes' in modData ? modData.attributes?.[key] : undefined
                    return (
                      <SelectItem key={key} value={key}>
                        {attrPool?.label}
                      </SelectItem>
                    )
                  })}
                </SelectContent>
              </Select>
            </div>
          )}

          {/* Result */}
          <div className="space-y-1.5">
            <Label>Результат</Label>
            <div className="flex items-center gap-2">
              <div
                className="flex h-10 flex-1 items-center rounded-md border border-input bg-muted px-3 text-sm font-medium"
                aria-live="polite"
              >
                {result ? (
                  <span className="text-foreground">{result}</span>
                ) : (
                  <span className="text-muted-foreground">
                    Нажмите &ldquo;Сгенерировать&rdquo; чтобы получить результат
                  </span>
                )}
              </div>
              {result && (
                <Button
                  size="icon"
                  variant="ghost"
                  className="h-10 w-10 shrink-0"
                  onClick={handleCopy}
                  title="Copy to clipboard"
                >
                  {copied ? (
                    <Check className="h-4 w-4 text-green-500" />
                  ) : (
                    <Copy className="h-4 w-4" />
                  )}
                </Button>
              )}
            </div>
          </div>

          {/* Live preview of token */}
          {category && subType && modifier && (
            <div className="flex items-center gap-2">
              <span className="text-xs text-muted-foreground">Токен:</span>
              <Badge variant="outline" className="font-mono text-xs">
                {attribute
                  ? `{{${category}.${subType}.${modifier}.${attribute}}}`
                  : `{{${category}.${subType}.${modifier}}}`
                }
              </Badge>
            </div>
          )}
        </div>

        <DialogFooter className="gap-2 sm:gap-2">
          <Button
            variant="outline"
            onClick={generate}
            disabled={!canGenerate || loading}
            className="gap-2"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Сгенерировать
          </Button>
          <Button onClick={handleInsert} disabled={!result}>
            Вставить
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
