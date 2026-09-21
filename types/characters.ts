// Character status values
export type CharacterStatus = "alive" | "dead" | "unknown" | "missing"

// Block types
export type BlockType = "status" | "text"

// Available optional status block identifiers
export type OptionalStatusBlockId = "семейное-положение"

// Layout modes
export type LayoutMode = "top" | "side"

// Base block interface
export interface BaseBlock {
  id: string
  type: BlockType
  label: string
}

// Status block - small, condensed fields
export interface StatusBlock extends BaseBlock {
  type: "status"
  value: string
}

// Text block - larger text content, collapsible (only one, non-removable)
export interface TextBlock extends BaseBlock {
  type: "text"
  content: string
  roles?: number[]
}

export type Block = StatusBlock | TextBlock

// Core status fields - all are deletable
export interface CoreStatusField {
  id: string
  label: string
  value: string
}

// Character data structure
export interface Character {
  id: number
  name: string
  description: string
  imageUrl: string | null
  statusFields: CoreStatusField[]
  textBlock: TextBlock
  optionalStatusBlocks: StatusBlock[]
  enabledOptionalStatusBlocks: OptionalStatusBlockId[]
  layoutMode: LayoutMode
  selectedRolesId: number[]
}

// Status block configuration for adding new status blocks
export interface StatusBlockConfig {
  id: OptionalStatusBlockId
  label: string
}

// Default status field configurations
export const DEFAULT_STATUS_FIELDS: Omit<CoreStatusField, "value">[] = [
  { id: "раса", label: "Раса" },
  { id: "возраст", label: "Возраст" },
  { id: "статус", label: "Статус" },
]

// Available optional status blocks configuration
export const OPTIONAL_STATUS_BLOCKS_CONFIG: StatusBlockConfig[] = [
  { id: "семейное-положение", label: "Семейное положение" },
]

// Edit context type for managing which field is being edited
export interface EditContext {
  blockId: string | null
  field: string | null
}

// Props for editable components
export interface EditableFieldProps {
  value: string
  isEditing: boolean
  onStartEdit: () => void
  onSave: (value: string) => void
  onCancel: () => void
  placeholder?: string
  canEdit?: boolean
  textSize?: string
}

export interface EditableTextAreaProps extends EditableFieldProps {
  rows?: number
}

// Export data for the character
export interface CharacterExportData {
  name: string
  description: string
  imageUrl: string | null
  statusFields: { label: string; value: string }[]
  textContent: { label: string; content: string }
  optionalStatusBlocks: { label: string; value: string }[]
  exportedAt: string
}
