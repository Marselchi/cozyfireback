"use client"

import { useState, useEffect, useCallback } from "react"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
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
import { Loader2, RefreshCw, AlertCircle } from "lucide-react"
import type { GeneratorData } from "@/types/generator"
import { useGeneratorData } from "@/hooks/use-generator-data"

interface PlaceholderDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Called when user confirms — receives the placeholder token to insert */
  onInsert: (token: string) => void
}

export function PlaceholderDialog({
  open,
  onOpenChange,
  onInsert,
}: Readonly<PlaceholderDialogProps>) {
  const { data, loading, error, fetchData, ensureData } = useGeneratorData()

  const [localData, setLocalData] = useState<GeneratorData | null>(null)

  const [category, setCategory] = useState<string>("")
  const [subType, setSubType] = useState<string>("")
  const [modifier, setModifier] = useState<string>("")
  const [attribute, setAttribute] = useState<string>("")

  // Load data when dialog opens
  useEffect(() => {
    if (!open) return
    ensureData().then((d) => {
      if (d) {
        setLocalData(d)
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

  useEffect(() => {
    if (data) setLocalData(data)
  }, [data])

  const handleCategoryChange = useCallback(
    (val: string) => {
      setCategory(val)
      setSubType("")
      setModifier("")
      setAttribute("")
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

  const token =
    category && subType && modifier
      ? attribute
        ? `{{${category}.${subType}.${modifier}.${attribute}}}`
        : `{{${category}.${subType}.${modifier}}}`
      : ""

  const handleConfirm = useCallback(() => {
    if (!token) return
    onInsert(token)
    onOpenChange(false)
  }, [token, onInsert, onOpenChange])

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
  const canConfirm = !!category && !!subType && !!modifier

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center justify-between gap-2">
            <span>Вставить плейсхолдер</span>
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
              Обновить список
            </Button>
          </DialogTitle>
          <DialogDescription>
            Выберите какой тип плейсхолдера вы хотите вставить для последующего
            &ldquo;Заполнения плейсхолдера&rdquo;.
          </DialogDescription>
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
            <Label htmlFor="ph-category">Категория</Label>
            <Select value={category} onValueChange={handleCategoryChange}>
              <SelectTrigger id="ph-category" className="w-full">
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
            <Label htmlFor="ph-subtype">Подтип</Label>
            <Select
              value={subType}
              onValueChange={handleSubTypeChange}
              disabled={!category}
            >
              <SelectTrigger id="ph-subtype" className="w-full">
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
            <Label htmlFor="ph-modifier">Модификатор</Label>
            <Select
              value={modifier}
              onValueChange={handleModifierChange}
              disabled={!subType}
            >
              <SelectTrigger id="ph-modifier" className="w-full">
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
              <Label htmlFor="ph-attribute">Атрибут (опционально)</Label>
              <Select
                value={attribute}
                onValueChange={(val) => setAttribute(val)}
                disabled={!modifier}
              >
                <SelectTrigger id="ph-attribute" className="w-full">
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

          {/* Preview */}
          {token && (
            <div className="rounded-md border border-dashed border-amber-500/60 bg-amber-500/10 px-4 py-3">
              <p className="mb-1 text-xs text-muted-foreground">
                Будет вставлено:
              </p>
              <code className="font-mono text-sm text-amber-600 dark:text-amber-400">
                {token}
              </code>
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Отмена
          </Button>
          <Button onClick={handleConfirm} disabled={!canConfirm}>
            Вставить
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
