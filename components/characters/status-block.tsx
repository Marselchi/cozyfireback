"use client"

import { X, Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { EditableField } from "./editable-field"
import { CoreStatusField, EditContext, OptionalStatusBlockId, StatusBlock, StatusBlockConfig } from "@/types/characters"

interface StatusFieldItemProps {
  field: CoreStatusField
  editContext: EditContext
  onStartEdit: (fieldId: string) => void
  onSave: (fieldId: string, value: string) => void
  onCancelEdit: () => void
  onDelete: (fieldId: string) => void
  canEdit?: boolean
}

function StatusFieldItem({
  field,
  editContext,
  onStartEdit,
  onSave,
  onCancelEdit,
  onDelete,
  canEdit = true,
}: Readonly<StatusFieldItemProps>) {
  const isEditing = editContext.blockId === "status" && editContext.field === field.id

  return (
    <div className="flex items-center gap-2 group/item ">
      <span className="text-sm font-medium text-muted-foreground min-w-15">
        {field.label}:
      </span>
      <span className="text-sm flex-1">
        <EditableField
          value={field.value}
          isEditing={isEditing}
          onStartEdit={() => onStartEdit(field.id)}
          onSave={(value) => onSave(field.id, value)}
          onCancel={onCancelEdit}
          placeholder="Неизвестно"
          canEdit={canEdit}
        />
      </span>
      {!isEditing && canEdit && (
        <Button
          variant="ghost"
          size="sm"
          className="h-6 w-6 p-0 opacity-0 group-hover/item:opacity-100 transition-opacity"
          onClick={() => onDelete(field.id)}
          aria-label={`Удалить ${field.label}`}
        >
          <X className="h-3 w-3" />
        </Button>
      )}
    </div>
  )
}

interface OptionalStatusBlockItemProps {
  block: StatusBlock
  editContext: EditContext
  onStartEdit: (blockId: string) => void
  onSave: (blockId: string, value: string) => void
  onCancelEdit: () => void
  onDelete: (blockId: string) => void
  canEdit?: boolean
}

function OptionalStatusBlockItem({
  block,
  editContext,
  onStartEdit,
  onSave,
  onCancelEdit,
  onDelete,
  canEdit = true,
}: Readonly<OptionalStatusBlockItemProps>) {
  const isEditing = editContext.blockId === "optionalStatus" && editContext.field === block.id

  return (
    <div className="flex items-center gap-2 group/item">
      <span className="text-sm font-medium text-muted-foreground min-w-15">
        {block.label}:
      </span>
      <span className="text-sm flex-1">
        <EditableField
          value={block.value}
          isEditing={isEditing}
          onStartEdit={() => onStartEdit(block.id)}
          onSave={(value) => onSave(block.id, value)}
          onCancel={onCancelEdit}
          placeholder="Не установлено"
          canEdit={canEdit}
        />
      </span>
      {!isEditing && canEdit && (
        <Button
          variant="ghost"
          size="sm"
          className="h-6 w-6 p-0 opacity-0 group-hover/item:opacity-100 transition-opacity"
          onClick={() => onDelete(block.id)}
          aria-label={`Удалить ${block.label}`}
        >
          <X className="h-3 w-3" />
        </Button>
      )}
    </div>
  )
}

interface StatusSectionProps {
  statusFields: CoreStatusField[]
  optionalStatusBlocks: StatusBlock[]
  enabledOptionalStatusBlocks: OptionalStatusBlockId[]
  availableStatusBlocksConfig: StatusBlockConfig[]
  editContext: EditContext
  onStartEditField: (fieldId: string) => void
  onSaveField: (fieldId: string, value: string) => void
  onDeleteField: (fieldId: string) => void
  onStartEditOptional: (blockId: string) => void
  onSaveOptional: (blockId: string, value: string) => void
  onDeleteOptional: (blockId: string) => void
  onAddStatusBlock: (blockId: OptionalStatusBlockId) => void
  onAddStatusField: (id: string, label: string) => void
  onCancelEdit: () => void
  /** When true, renders without a section heading and uses a single-column stacked layout */
  compact?: boolean
  canEdit?: boolean
}

export function StatusSection({
  statusFields,
  optionalStatusBlocks,
  enabledOptionalStatusBlocks,
  availableStatusBlocksConfig,
  editContext,
  onStartEditField,
  onSaveField,
  onDeleteField,
  onStartEditOptional,
  onSaveOptional,
  onDeleteOptional,
  onAddStatusBlock,
  onAddStatusField,
  onCancelEdit,
  compact = false,
  canEdit = true,
}: Readonly<StatusSectionProps>) {
  const hasAnyStatus = statusFields.length > 0 || optionalStatusBlocks.length > 0
  const availableOptionalBlocks = availableStatusBlocksConfig.filter(
    (config) => !enabledOptionalStatusBlocks.includes(config.id)
  )

  // If no statuses and no way to add, don't render
  if (!hasAnyStatus && availableOptionalBlocks.length === 0) {
    return null
  }

  const addButton = canEdit && (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="outline" size="sm" className="gap-1">
          <Plus className="h-3 w-3" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem
          onClick={() => {
            const id = `custom-${Date.now()}`
            const label = prompt("Enter status label:")
            if (label) onAddStatusField(id, label)
          }}
        >
          Кастомный статус
        </DropdownMenuItem>
        {availableOptionalBlocks.map((config) => (
          <DropdownMenuItem
            key={config.id}
            onClick={() => onAddStatusBlock(config.id)}
          >
            {config.label}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  )

  const fieldList = hasAnyStatus && (
    <div
      className={
        compact
          ? "flex flex-col gap-2 p-3 bg-muted/30 rounded-lg"
          : "grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4 p-4 bg-muted/30 rounded-lg"
      }
    >
      {statusFields.map((field) => (
        <StatusFieldItem
          key={field.id}
          field={field}
          editContext={editContext}
          onStartEdit={onStartEditField}
          onSave={onSaveField}
          onCancelEdit={onCancelEdit}
          onDelete={onDeleteField}
          canEdit={canEdit}
        />
      ))}
      {optionalStatusBlocks.map((block) => (
        <OptionalStatusBlockItem
          key={block.id}
          block={block}
          editContext={editContext}
          onStartEdit={onStartEditOptional}
          onSave={onSaveOptional}
          onCancelEdit={onCancelEdit}
          onDelete={onDeleteOptional}
          canEdit={canEdit}
        />
      ))}
    </div>
  )

  // Compact mode: no heading, stacked layout, used inside the sidebar
  if (compact) {
    return (
      <div className="space-y-2">
        {fieldList}
        <div className="flex justify-end">{addButton}</div>
      </div>
    )
  }

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold">Статус</h2>
        {addButton}
      </div>
      {fieldList}
    </section>
  )
}
