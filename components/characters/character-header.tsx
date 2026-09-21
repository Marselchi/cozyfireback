"use client"

import Image from "next/image"
import { memo, type ReactNode } from "react"
import { EditableField, EditableTextArea } from "./editable-field"
import { StatusSection } from "./status-block"
import {
  LayoutMode,
  EditContext,
  CoreStatusField,
  OptionalStatusBlockId,
  StatusBlockConfig,
  StatusBlock,
} from "@/types/characters"

interface CharacterHeaderProps {
  name: string
  description: string
  imageUrl: string | null
  layoutMode: LayoutMode
  editContext: EditContext
  onStartEdit: (field: string) => void
  onSave: (field: "name" | "description" | "imageUrl", value: string) => void
  onCancelEdit: () => void
  canEdit?: boolean
}

export const CharacterHeader = memo(function CharacterHeader({
  name,
  description,
  imageUrl,
  layoutMode,
  editContext,
  onStartEdit,
  onSave,
  onCancelEdit,
  canEdit = true,
}: CharacterHeaderProps) {
  const isEditingName =
    editContext.blockId === "header" && editContext.field === "name"
  const isEditingDescription =
    editContext.blockId === "header" && editContext.field === "description"

  const imageElement = imageUrl && (
    <div className="flex-shrink-0">
      <div className="relative w-48 h-48 md:w-64 md:h-64 rounded-lg overflow-hidden border bg-muted">
        <Image
          src={imageUrl}
          alt={name || "Character portrait"}
          fill
          className="object-cover"
          priority
        />
      </div>
    </div>
  )

  const nameElement = (
    <h1 className="text-3xl md:text-4xl font-bold">
      <EditableField
        value={name}
        isEditing={isEditingName}
        onStartEdit={() => onStartEdit("name")}
        onSave={(value) => onSave("name", value)}
        onCancel={onCancelEdit}
        placeholder="Имя персонажа"
        canEdit={canEdit}
        textSize="lg"
      />
    </h1>
  )

  const descriptionElement = (
    <div className="text-muted-foreground border-b-2 border-input">
      <EditableTextArea
        value={description}
        isEditing={isEditingDescription}
        onStartEdit={() => onStartEdit("description")}
        onSave={(value) => onSave("description", value)}
        onCancel={onCancelEdit}
        placeholder="Описание..."
        rows={3}
        canEdit={canEdit}
      />
    </div>
  )

  if (layoutMode === "top") {
    return (
      <header className="flex flex-col md:flex-row gap-6 pb-6">
        {imageElement}
        <div className="flex-1 space-y-4">
          {nameElement}
          {descriptionElement}
        </div>
      </header>
    )
  }

  return null
})

interface CharacterSidebarProps {
  name: string
  description: string
  imageUrl: string | null
  editContext: EditContext
  onStartEditHeader: (field: string) => void
  onSaveHeader: (field: "name" | "description" | "imageUrl", value: string) => void
  onCancelEdit: () => void
  rolesSection?: ReactNode
  statusFields: CoreStatusField[]
  optionalStatusBlocks: StatusBlock[]
  enabledOptionalStatusBlocks: OptionalStatusBlockId[]
  availableStatusBlocksConfig: StatusBlockConfig[]
  onStartEditField: (fieldId: string) => void
  onSaveField: (fieldId: string, value: string) => void
  onDeleteField: (fieldId: string) => void
  onStartEditOptional: (blockId: string) => void
  onSaveOptional: (blockId: string, value: string) => void
  onDeleteOptional: (blockId: string) => void
  onAddStatusBlock: (blockId: OptionalStatusBlockId) => void
  onAddStatusField: (id: string, label: string) => void
  canEdit?: boolean
}

export const CharacterSidebar = memo(function CharacterSidebar({
  name,
  description,
  imageUrl,
  editContext,
  onStartEditHeader,
  onSaveHeader,
  onCancelEdit,
  rolesSection,
  statusFields,
  optionalStatusBlocks,
  enabledOptionalStatusBlocks,
  availableStatusBlocksConfig,
  onStartEditField,
  onSaveField,
  onDeleteField,
  onStartEditOptional,
  onSaveOptional,
  onDeleteOptional,
  onAddStatusBlock,
  onAddStatusField,
  canEdit = true,
}: Readonly<CharacterSidebarProps>) {
  const isEditingName =
    editContext.blockId === "header" && editContext.field === "name"
  const isEditingDescription =
    editContext.blockId === "header" && editContext.field === "description"

  return (
    <aside className="w-full md:w-72 shrink-0 space-y-4">
      {imageUrl ? (
        <div className="relative w-full aspect-square rounded-lg overflow-hidden border bg-muted">
          <Image
            src={imageUrl}
            alt={name || "Character portrait"}
            fill
            className="object-cover"
            priority
          />
        </div>
      ) : (
        <div className="div"></div>
        // <div className="relative w-full aspect-square rounded-lg overflow-hidden border bg-muted flex items-center justify-center">
        //   <span className="text-muted-foreground text-sm">Нет изображsfdия</span>
        // </div>
      )}

      <div className="border-b border-input pb-3 relative">
        <h1 className="text-2xl font-bold">
          <EditableField
            value={name}
            isEditing={isEditingName}
            onStartEdit={() => onStartEditHeader("name")}
            onSave={(value) => onSaveHeader("name", value)}
            onCancel={onCancelEdit}
            placeholder="Имя персонажа"
            canEdit={canEdit}
            textSize="lg"
          />
        </h1>
      </div>

      <div className="text-sm text-muted-foreground pb-3 relative">
        <EditableTextArea
          value={description}
          isEditing={isEditingDescription}
          onStartEdit={() => onStartEditHeader("description")}
          onSave={(value) => onSaveHeader("description", value)}
          onCancel={onCancelEdit}
          placeholder="Описание..."
          rows={3}
          canEdit={canEdit}
        />
      </div>

      {rolesSection}

      <StatusSection
        statusFields={statusFields}
        optionalStatusBlocks={optionalStatusBlocks}
        enabledOptionalStatusBlocks={enabledOptionalStatusBlocks}
        availableStatusBlocksConfig={availableStatusBlocksConfig}
        editContext={editContext}
        onStartEditField={onStartEditField}
        onSaveField={onSaveField}
        onDeleteField={onDeleteField}
        onStartEditOptional={onStartEditOptional}
        onSaveOptional={onSaveOptional}
        onDeleteOptional={onDeleteOptional}
        onAddStatusBlock={onAddStatusBlock}
        onAddStatusField={onAddStatusField}
        onCancelEdit={onCancelEdit}
        compact
        canEdit={canEdit}
      />
    </aside>
  )
})