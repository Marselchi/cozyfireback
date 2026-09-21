"use client"

import { useState, useCallback, useEffect, useRef, useMemo } from "react"
import { LayoutPanelLeft, MessageSquareText, X, Search, Save } from "lucide-react"
import { Button } from "@/components/ui/button"
import { CharacterHeader, CharacterSidebar } from "./character-header"
import { StatusSection } from "./status-block"
import { TextBlock } from "./text-block"
import {
  LayoutMode,
  Character,
  EditContext,
  OptionalStatusBlockId,
  OPTIONAL_STATUS_BLOCKS_CONFIG,
  CharacterExportData,
  CoreStatusField,
  StatusBlock,
} from "@/types/characters"
import { cn } from "@/lib/utils"
import { Badge } from "@/components/ui/badge"
import { QuestionPanel } from "@/components/lore/questions/question-panel"
import { TextSelectionPopup } from "@/components/lore/questions/text-selection-popup"
import { useQuoteHighlight } from "@/hooks/use-quote-highlight"
import type { Role } from "@/types/editor-layout"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { ScrollArea, ScrollBar } from "@/components/ui/scroll-area"
import { createCharacter, updateCharacter } from "@/server/characters/characters"
import { useRouter } from "next/navigation"
import type { Excerpt } from "@/types/lore"

const LAYOUT_STORAGE_KEY = "character-layout-preference"

function createStatusField(id: string, label: string): CoreStatusField {
  return {
    id,
    label,
    value: "",
  }
}

function createStatusBlock(id: string, label: string): StatusBlock {
  return {
    id,
    type: "status",
    label,
    value: "",
  }
}

function getStoredLayout(): LayoutMode | null {
  if (globalThis.window === undefined) return null
  const stored = localStorage.getItem(LAYOUT_STORAGE_KEY)
  return stored === "top" || stored === "side" ? stored : null
}

function storeLayout(layout: LayoutMode) {
  if (globalThis.window === undefined) return
  localStorage.setItem(LAYOUT_STORAGE_KEY, layout)
}

function areRoleArraysEqual(a: number[], b: number[]) {
  if (a.length !== b.length) return false
  const sortedA = [...a].sort((x, y) => x - y)
  const sortedB = [...b].sort((x, y) => x - y)
  return sortedA.every((value, index) => value === sortedB[index])
}

interface CharacterDisplayProps {
  initialData: Character
  characterId?: number
  questionCount?: number
  excerpts?: Excerpt[]
  roles: Role[]
  roomName: string
  isAuthor?: boolean
}

export function CharacterDisplay({
  initialData,
  characterId,
  questionCount = 0,
  excerpts = [],
  roles,
  roomName,
  isAuthor = true,
}: Readonly<CharacterDisplayProps>) {
  const router = useRouter()
  const isCreateMode = characterId === 0
  const canEdit = isAuthor || isCreateMode
  // Server + first client render must be identical.
  // So we never read localStorage in the state initializer.
  const [character, setCharacter] = useState<Character>(initialData)
  const [layoutHydrated, setLayoutHydrated] = useState(false)
  const [roleSearch, setRoleSearch] = useState("")
  const [hasRoleChanges, setHasRoleChanges] = useState(false)
  const [isRolesEditing, setIsRolesEditing] = useState(false)
  const [pendingRoles, setPendingRoles] = useState<number[]>(
    initialData.selectedRolesId || []
  )
  const [isSaving, setIsSaving] = useState(false)

  // Track if any changes have been made
  const [hasChanges, setHasChanges] = useState(false)

  // Check if current character differs from initialData
  useEffect(() => {
    if (isCreateMode) {
      // In create mode, any non-empty field means changes
      const isDirty = 
        character.name !== "" ||
        character.description !== "" ||
        character.textBlock.content !== "" ||
        character.statusFields.some(f => f.value !== "") ||
        character.optionalStatusBlocks.some(b => b.value !== "") ||
        !areRoleArraysEqual(character.selectedRolesId || [], initialData.selectedRolesId || [])
      setHasChanges(isDirty)
      return
    }

    // In edit mode, compare with initialData
    const isDirty = 
      character.name !== initialData.name ||
      character.description !== initialData.description ||
      character.textBlock.content !== initialData.textBlock.content ||
      JSON.stringify(character.statusFields) !== JSON.stringify(initialData.statusFields) ||
      JSON.stringify(character.optionalStatusBlocks) !== JSON.stringify(initialData.optionalStatusBlocks) ||
      !areRoleArraysEqual(character.selectedRolesId || [], initialData.selectedRolesId || [])
    
    setHasChanges(isDirty)
  }, [character, initialData, isCreateMode])

  useEffect(() => {
    setCharacter(initialData)
  }, [initialData, characterId])

  // Синхронизация pendingRoles при загрузке character
  useEffect(() => {
    setPendingRoles(character.selectedRolesId || [])
    setHasRoleChanges(false)
    setIsRolesEditing(false)
  }, [character.selectedRolesId])

  // Read layout only after mount, then apply it if it exists.
  useEffect(() => {
    const storedLayout = getStoredLayout()

    if (storedLayout) {
      setCharacter((prev) =>
        prev.layoutMode === storedLayout ? prev : { ...prev, layoutMode: storedLayout }
      )
    }

    setLayoutHydrated(true)
  }, [])

  // Persist only after we have resolved the client layout.
  useEffect(() => {
    if (!layoutHydrated) return
    storeLayout(character.layoutMode)
  }, [character.layoutMode, layoutHydrated])

  const [questionMode, setQuestionMode] = useState(false)
  const characterContainerRef = useRef<HTMLDivElement>(null)
  const { scrollToQuote } = useQuoteHighlight(characterContainerRef)

  const handleQuoteClick = useCallback(
    (quoteText: string) => {
      scrollToQuote(quoteText)
    },
    [scrollToQuote]
  )

  const [editContext, setEditContext] = useState<EditContext>({
    blockId: null,
    field: null,
  })

  const isEditingAny = editContext.blockId !== null

  const handleStartEdit = useCallback(
    (blockId: string, field: string) => {
      if (isEditingAny || !canEdit) return
      setEditContext({ blockId, field })
    },
    [isEditingAny, canEdit]
  )

  const handleCancelEdit = useCallback(() => {
    setEditContext({ blockId: null, field: null })
  }, [])

  const handleSaveAll = useCallback(async () => {
    if (!hasChanges && !isCreateMode) return
    if (!character.name.trim()) {
      alert("Имя персонажа обязательно")
      return
    }

    setIsSaving(true)

    // Serialize status fields to API format
    const statusString = character.statusFields
      .filter(f => f.value)
      .map(f => `${f.label.toLowerCase()}:${f.value}`)
      .join(';')

    try {
      if (isCreateMode) {
        const result = await createCharacter({
          roomName,
          name: character.name.trim(),
          description: character.description || undefined,
          status: statusString || undefined,
          content: character.textBlock.content || undefined,
          roleIds: character.selectedRolesId.length > 0 ? character.selectedRolesId : undefined,
        })

        if (result.success && result.id) {
          setHasChanges(false)
          router.push(`/rooms/${roomName}/characters/${result.id}`)
        } else {
          alert("Ошибка при создании персонажа")
        }
      } else {
        const result = await updateCharacter({
          id: character.id,
          roomName,
          name: character.name,
          description: character.description,
          status: statusString,
          content: character.textBlock.content,
          roleIds: character.selectedRolesId,
        })

        if (result.success) {
          setHasChanges(false)
          // Close edit mode if any
          setEditContext({ blockId: null, field: null })
        } else {
          alert("Ошибка при сохранении персонажа")
        }
      }
    } catch (error) {
      console.error("Save error:", error)
      alert("Произошла ошибка при сохранении")
    } finally {
      setIsSaving(false)
    }
  }, [hasChanges, isCreateMode, character, roomName, router])

  const handleSaveHeader = useCallback(
    (field: "name" | "description" | "imageUrl", value: string) => {
      setCharacter((prev) => ({
        ...prev,
        [field]: value,
      }))
      setEditContext({ blockId: null, field: null })
    },
    []
  )

  const handleSaveStatusField = useCallback((fieldId: string, value: string) => {
    setCharacter((prev) => ({
      ...prev,
      statusFields: prev.statusFields.map((field) =>
        field.id === fieldId ? { ...field, value } : field
      ),
    }))
    setEditContext({ blockId: null, field: null })
  }, [])

  const handleDeleteStatusField = useCallback((fieldId: string) => {
    setCharacter((prev) => ({
      ...prev,
      statusFields: prev.statusFields.filter((field) => field.id !== fieldId),
    }))
    setEditContext((prev) =>
      prev.blockId === "status" && prev.field === fieldId
        ? { blockId: null, field: null }
        : prev
    )
  }, [])

  const handleAddStatusField = useCallback((id: string, label: string) => {
    const newField = createStatusField(id, label)
    setCharacter((prev) => ({
      ...prev,
      statusFields: [...prev.statusFields, newField],
    }))
  }, [])

  const handleSaveOptionalStatus = useCallback((blockId: string, value: string) => {
    setCharacter((prev) => ({
      ...prev,
      optionalStatusBlocks: prev.optionalStatusBlocks.map((block) =>
        block.id === blockId ? { ...block, value } : block
      ),
    }))
    setEditContext({ blockId: null, field: null })
  }, [])

  const handleDeleteOptionalStatus = useCallback((blockId: string) => {
    setCharacter((prev) => ({
      ...prev,
      optionalStatusBlocks: prev.optionalStatusBlocks.filter(
        (block) => block.id !== blockId
      ),
      enabledOptionalStatusBlocks: prev.enabledOptionalStatusBlocks.filter(
        (id) => id !== blockId
      ),
    }))
    setEditContext((prev) =>
      prev.blockId === "optionalStatus" && prev.field === blockId
        ? { blockId: null, field: null }
        : prev
    )
  }, [])

  const handleAddOptionalStatusBlock = useCallback(
    (blockId: OptionalStatusBlockId) => {
      const config = OPTIONAL_STATUS_BLOCKS_CONFIG.find((c) => c.id === blockId)
      if (!config) return

      const newBlock = createStatusBlock(config.id, config.label)

      setCharacter((prev) => ({
        ...prev,
        optionalStatusBlocks: [...prev.optionalStatusBlocks, newBlock],
        enabledOptionalStatusBlocks: [...prev.enabledOptionalStatusBlocks, blockId],
      }))
    },
    []
  )

  const handleSaveTextBlock = useCallback((content: string) => {
    setCharacter((prev) => ({
      ...prev,
      textBlock: { ...prev.textBlock, content },
    }))
    setEditContext({ blockId: null, field: null })
  }, [])

  const toggleRole = useCallback(
    (roleId: number) => {
      setPendingRoles((prev) => {
        const newRoles = prev.includes(roleId)
          ? prev.filter((id) => id !== roleId)
          : [...prev, roleId]

        setHasRoleChanges(
          !areRoleArraysEqual(newRoles, character.selectedRolesId || [])
        )

        return newRoles
      })
    },
    [character.selectedRolesId]
  )

  const handleSaveRoles = useCallback(() => {
    setCharacter((prev) => ({
      ...prev,
      selectedRolesId: pendingRoles,
    }))
    setHasRoleChanges(false)
    setIsRolesEditing(false)
  }, [pendingRoles])

  const handleCancelRoles = useCallback(() => {
    setPendingRoles(character.selectedRolesId || [])
    setHasRoleChanges(false)
    setIsRolesEditing(false)
  }, [character.selectedRolesId])

  const filteredRoles = useMemo(() => {
    if (!roleSearch.trim()) return roles
    return roles.filter((role) =>
      role.name.toLowerCase().includes(roleSearch.toLowerCase())
    )
  }, [roles, roleSearch])

  const selectedRolesData = useMemo(() => {
    return roles.filter((role) => pendingRoles.includes(role.id))
  }, [roles, pendingRoles])

  const RolesSection = ({ compact = false }: { compact?: boolean }) => {
    const showHorizontalScroll = !compact

    return (
      <div className={cn("space-y-3", compact ? "" : "mt-4")}>
        <div className="flex items-center justify-between gap-2">
          <Label>Роли</Label>

          {!isRolesEditing ? (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => setIsRolesEditing(true)}
            >
              Редактировать
            </Button>
          ) : (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => {
                setPendingRoles(character.selectedRolesId || [])
                setHasRoleChanges(false)
                setIsRolesEditing(false)
              }}
            >
              Закрыть
            </Button>
          )}
        </div>

        {!isRolesEditing ? (
          <div className="rounded-md border-b-2 border-input bg-background mb-2">
            {selectedRolesData.length > 0 ? (
              <div
                className={cn(
                  showHorizontalScroll ? "overflow-x-auto pb-2" : "",
                  "max-w-full"
                )}
              >
                <ul
                  className={cn(
                    "p-3",
                    showHorizontalScroll
                      ? "flex w-max flex-nowrap gap-2"
                      : "space-y-2"
                  )}
                >
                  {selectedRolesData.map((role) => (
                    <li
                      key={role.id}
                      className={cn(
                        "rounded-md border-input border bg-accent px-3 py-2 text-sm",
                        showHorizontalScroll
                          ? "shrink-0 whitespace-nowrap"
                          : "w-full"
                      )}
                    >
                      {role.name}
                    </li>
                  ))}
                </ul>
              </div>
            ) : (
              <p className="p-3 text-sm text-muted-foreground">Роли не выбраны</p>
            )}
          </div>
        ) : (
          <>
            <div className="relative">
              <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Поиск ролей..."
                value={roleSearch}
                onChange={(e) => setRoleSearch(e.target.value)}
                className="pl-8"
              />
            </div>

            {selectedRolesData.length > 0 && (
              <div className="rounded-md border border-border p-2">
                <p className="pb-2 text-xs text-muted-foreground">Выбраны:</p>
                <ul className="space-y-2">
                  {selectedRolesData.map((role) => (
                    <li key={role.id}>
                      <button
                        type="button"
                        onClick={() => toggleRole(role.id)}
                        className="flex w-full items-center justify-between rounded-md border border-primary/30 bg-primary/10 px-3 py-2 text-left text-sm transition-colors hover:bg-primary/15"
                      >
                        <span>{role.name}</span>
                        <X className="h-4 w-4" />
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            <ScrollArea className="rounded-md border border-border h-40">
              <div className="p-2">
                <ul className="space-y-2">
                  {filteredRoles.map((role) => {
                    const isSelected = pendingRoles.includes(role.id)

                    return (
                      <li key={role.id}>
                        <button
                          type="button"
                          onClick={() => toggleRole(role.id)}
                          className={cn(
                            "w-full rounded-md border p-2 text-left transition-colors text-sm",
                            isSelected
                              ? "border-primary bg-primary/10"
                              : "border-border hover:bg-accent bg-input/30"
                          )}
                        >
                          <div className="font-medium">{role.name}</div>
                          {role.users && role.users.length > 0 && (
                            <div className="flex flex-wrap gap-1 pt-1">
                              <span className="text-xs text-muted-foreground">
                                Владеют:
                              </span>
                              {role.users.map((userName, index) => (
                                <Badge
                                  key={index + userName}
                                  variant="secondary"
                                  className="text-xs h-5"
                                >
                                  {userName}
                                </Badge>
                              ))}
                            </div>
                          )}
                        </button>
                      </li>
                    )
                  })}
                  {filteredRoles.length === 0 && (
                    <li className="p-2 text-sm text-muted-foreground">
                      Ролей не найдено
                    </li>
                  )}
                </ul>
              </div>
              <ScrollBar />
            </ScrollArea>

            {hasRoleChanges && (
              <div className="flex gap-2 pt-1">
                <Button onClick={handleSaveRoles} size="sm" className="flex-1">
                  Сохранить
                </Button>
                <Button
                  onClick={handleCancelRoles}
                  size="sm"
                  variant="outline"
                  className="flex-1"
                >
                  Отмена
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    )
  }

  const handleToggleLayout = useCallback(() => {
    if (isEditingAny) return
    setCharacter((prev) => ({
      ...prev,
      layoutMode: prev.layoutMode === "top" ? "side" : "top",
    }))
  }, [isEditingAny])

  const floatingControls = (
    <div className="fixed right-4 bottom-4 z-40">
      <Button
        variant="outline"
        size="sm"
        className="gap-2 mr-2"
        onClick={handleToggleLayout}
        disabled={isEditingAny}
        title={isEditingAny ? "Finish editing first" : "Switch layout"}
      >
        <LayoutPanelLeft className="h-4 w-4" />
        Сменить стиль
      </Button>

      <Button
        onClick={() => setQuestionMode((v) => !v)}
        variant={questionMode ? "default" : "outline"}
        size="sm"
        className={cn(
          "gap-2 shadow-md transition-all",
          questionMode && "bg-primary text-primary-foreground"
        )}
      >
        {questionMode ? (
          <>
            <X className="w-4 h-4" />
            Закрыть вопросы
          </>
        ) : (
          <div className="relative inline-flex items-center">
            <MessageSquareText className="w-4 h-4" />
            <span>Вопросы</span>

            {questionCount > 0 && (
              <Badge className="absolute -top-3 -left-5 h-5 w-5 p-0 flex items-center justify-center text-sm pointer-events-none z-10">
                {questionCount}
              </Badge>
            )}
          </div>
        )}
      </Button>
    </div>
  )

  const floatingSaveButton = (hasChanges && canEdit && !questionMode) && (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50">
      <Button
        onClick={handleSaveAll}
        disabled={isSaving || isEditingAny}
        size="lg"
        variant={"outline"}
        className={cn(
          "gap-2 shadow-lg transition-all",
          "animate-in fade-in slide-in-from-bottom-2 duration-200"
        )}
      >
        <Save className="h-4 w-4" />
        {isSaving ? "Сохранение..." : isCreateMode ? "Создать персонажа" : "Сохранить изменения"}
      </Button>
    </div>
  )

  const isSideLayout = character.layoutMode === "side"

  if (isSideLayout) {
    return (
        <div className="min-h-screen">
          {floatingControls}
          {floatingSaveButton}

          <div
            className={cn(
              "transition-all duration-300 ease-in-out",
              questionMode ? "flex" : ""
            )}
          >
            <div
              className={cn(
                "transition-all duration-300 ease-in-out",
                questionMode ? "w-1/2 border-r border-border" : "w-full"
              )}
            >
              <div
                ref={characterContainerRef}
                className="max-w-6xl mx-auto p-6 flex flex-col md:flex-row gap-8"
              >
                <CharacterSidebar
                  name={character.name}
                  description={character.description}
                  imageUrl={character.imageUrl}
                  editContext={editContext}
                  onStartEditHeader={(field) => handleStartEdit("header", field)}
                  onSaveHeader={handleSaveHeader}
                  onCancelEdit={handleCancelEdit}
                  rolesSection={canEdit ? <RolesSection compact /> : null}
                  statusFields={character.statusFields}
                  optionalStatusBlocks={character.optionalStatusBlocks}
                  enabledOptionalStatusBlocks={character.enabledOptionalStatusBlocks}
                  availableStatusBlocksConfig={OPTIONAL_STATUS_BLOCKS_CONFIG}
                  onStartEditField={(fieldId) => handleStartEdit("status", fieldId)}
                  onSaveField={handleSaveStatusField}
                  onDeleteField={handleDeleteStatusField}
                  onStartEditOptional={(blockId) =>
                    handleStartEdit("optionalStatus", blockId)
                  }
                  onSaveOptional={handleSaveOptionalStatus}
                  onDeleteOptional={handleDeleteOptionalStatus}
                  onAddStatusBlock={handleAddOptionalStatusBlock}
                  onAddStatusField={handleAddStatusField}
                  canEdit={canEdit}
                />

                <div className="flex-1 space-y-8">
                  <TextBlock
                    block={character.textBlock}
                    editContext={editContext}
                    onStartEdit={() => handleStartEdit("textBlock", "content")}
                    onSave={handleSaveTextBlock}
                    onCancelEdit={handleCancelEdit}
                    excerpts={excerpts}
                    canEdit={canEdit}
                    roles={roles}
                  />
                </div>
              </div>
            </div>

            {questionMode && (
              <div className="w-1/2 border-l border-border bg-background fixed right-0 top-(--navbar-height,0px) h-[calc(100vh-var(--navbar-height,0px))] overflow-hidden animate-in slide-in-from-right-5 duration-300">
                <QuestionPanel
                  characterId={characterId?.toString()}
                  onQuoteClick={handleQuoteClick}
                />
              </div>
            )}
          </div>

          <TextSelectionPopup containerRef={characterContainerRef} enabled={questionMode} />
        </div>
    )
  }

  return (
      <div className="min-h-screen">
        {floatingControls}
        {floatingSaveButton}

        <div
          className={cn(
            "transition-all duration-300 ease-in-out",
            questionMode ? "flex gap-0" : ""
          )}
        >
          <div
            className={cn(
              "transition-all duration-300 ease-in-out",
              questionMode ? "w-1/2 border-r border-border min-h-screen" : "w-full"
            )}
          >
            <div
              className={cn(
                "mx-auto px-4 py-8 relative",
                questionMode ? "max-w-none" : "max-w-4xl"
              )}
            >
              <div ref={characterContainerRef} className="relative">
                <CharacterHeader
                  name={character.name}
                  description={character.description}
                  imageUrl={character.imageUrl}
                  layoutMode={character.layoutMode}
                  editContext={editContext}
                  onStartEdit={(field) => handleStartEdit("header", field)}
                  onSave={handleSaveHeader}
                  onCancelEdit={handleCancelEdit}
                  canEdit={canEdit}
                />

                {canEdit && <RolesSection />}

                <StatusSection
                  statusFields={character.statusFields}
                  optionalStatusBlocks={character.optionalStatusBlocks}
                  enabledOptionalStatusBlocks={character.enabledOptionalStatusBlocks}
                  availableStatusBlocksConfig={OPTIONAL_STATUS_BLOCKS_CONFIG}
                  editContext={editContext}
                  onStartEditField={(fieldId) => handleStartEdit("status", fieldId)}
                  onSaveField={handleSaveStatusField}
                  onDeleteField={handleDeleteStatusField}
                  onStartEditOptional={(blockId) =>
                    handleStartEdit("optionalStatus", blockId)
                  }
                  onSaveOptional={handleSaveOptionalStatus}
                  onDeleteOptional={handleDeleteOptionalStatus}
                  onAddStatusBlock={handleAddOptionalStatusBlock}
                  onAddStatusField={handleAddStatusField}
                  onCancelEdit={handleCancelEdit}
                  canEdit={canEdit}
                />

                <div className="border-t-2 border-input mt-2">
                  <TextBlock
                    block={character.textBlock}
                    editContext={editContext}
                    onStartEdit={() => handleStartEdit("textBlock", "content")}
                    onSave={handleSaveTextBlock}
                    onCancelEdit={handleCancelEdit}
                    excerpts={excerpts}
                    canEdit={canEdit}
                    roles={roles}
                  />
                </div>

                <TextSelectionPopup
                  containerRef={characterContainerRef}
                  enabled={questionMode}
                />
              </div>
            </div>
          </div>

          {questionMode && (
            <div
              className="w-1/2 min-h-screen border-l border-border bg-background sticky h-screen overflow-hidden animate-in slide-in-from-right-5 duration-300"
              style={{ top: "var(--navbar-height, 0px)" }}
            >
              <QuestionPanel
                characterId={characterId?.toString()}
                onQuoteClick={handleQuoteClick}
              />
            </div>
          )}
        </div>
      </div>
  )
}