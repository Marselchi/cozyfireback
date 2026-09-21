/**
 * Context menu item registry.
 *
 * Every entry that can appear in the editor's right-click menu is declared
 * here exactly once. Order is determined by presets, not by this file.
 * Visibility that used to live as JSX conditionals (`{isOnLink && ...}`) now
 * lives as a `visible(ctx)` predicate, so the rendering component stays a
 * dumb walker over the preset.
 */

import {
  PlusSquare,
  Link,
  Pencil,
  Unlink,
  Wand2,
  BookmarkPlus,
  Replace,
  LayoutTemplate,
  FolderOpen,
  Columns3,
  Rows3,
  Trash2,
  Table2,
} from "lucide-react";

import type { ContextMenuItemDefinition } from "./types";

// ---------------------------------------------------------------------------
// Spoiler
// ---------------------------------------------------------------------------

const spoiler: ContextMenuItemDefinition = {
  kind: "action",
  id: "spoiler",
  icon: PlusSquare,
  label: "Спойлер",
  visible: (ctx) => ctx.canInsertInlineEditor,
  onSelect: (ctx) => ctx.onAddInlineEditor(),
};

// ---------------------------------------------------------------------------
// Link group
// ---------------------------------------------------------------------------

const addLink: ContextMenuItemDefinition = {
  kind: "action",
  id: "add-link",
  icon: Link,
  label: "Ссылка",
  visible: (ctx) => !ctx.isOnLink,
  onSelect: (ctx) => ctx.onAddLink(),
};

const changeUrl: ContextMenuItemDefinition = {
  kind: "action",
  id: "change-url",
  icon: Pencil,
  label: "Изменить URL",
  visible: (ctx) => ctx.isOnLink && ctx.isEditableLink,
  onSelect: (ctx) => ctx.onChangeUrl(),
};

const removeLink: ContextMenuItemDefinition = {
  kind: "action",
  id: "remove-link",
  icon: Unlink,
  label: "Удалить ссылку",
  variant: "destructive",
  visible: (ctx) => ctx.isOnLink,
  onSelect: (ctx) => ctx.onRemoveLink(),
};

// ---------------------------------------------------------------------------
// Table group
// ---------------------------------------------------------------------------

const insertTable: ContextMenuItemDefinition = {
  kind: "action",
  id: "insert-table",
  icon: Table2,
  label: "Добавить таблицу",
  visible: (ctx) => !ctx.isInTableCell,
  onSelect: (ctx) => ctx.onInsertTable(),
};

const deleteRow: ContextMenuItemDefinition = {
  kind: "action",
  id: "delete-row",
  icon: Rows3,
  label: "Удалить строку",
  variant: "destructive",
  visible: (ctx) => ctx.isInTableCell,
  onSelect: (ctx) => ctx.onDeleteRow(),
};

const deleteColumn: ContextMenuItemDefinition = {
  kind: "action",
  id: "delete-column",
  icon: Columns3,
  label: "Удалить столбец",
  variant: "destructive",
  visible: (ctx) => ctx.isInTableCell,
  onSelect: (ctx) => ctx.onDeleteColumn(),
};

const deleteTable: ContextMenuItemDefinition = {
  kind: "action",
  id: "delete-table",
  icon: Trash2,
  label: "Удалить таблицу",
  variant: "destructive",
  visible: (ctx) => ctx.isInTableCell,
  onSelect: (ctx) => ctx.onDeleteTable(),
};

// ---------------------------------------------------------------------------
// Generator group
// ---------------------------------------------------------------------------

const generate: ContextMenuItemDefinition = {
  kind: "action",
  id: "generate",
  icon: Wand2,
  label: "Сгенерировать",
  visible: (ctx) => ctx.showGeneratorOption,
  onSelect: (ctx) => ctx.onOpenGenerator(),
};

const placeholder: ContextMenuItemDefinition = {
  kind: "action",
  id: "placeholder",
  icon: BookmarkPlus,
  label: "Плейсхолдер",
  visible: (ctx) => ctx.showGeneratorOption,
  onSelect: (ctx) => ctx.onOpenPlaceholder(),
};

const fillPlaceholders: ContextMenuItemDefinition = {
  kind: "action",
  id: "fill-placeholders",
  icon: Replace,
  label: "Заполнить плейсхолдеры",
  visible: (ctx) => ctx.showGeneratorOption,
  onSelect: (ctx) => ctx.onFillPlaceholders(),
};

// ---------------------------------------------------------------------------
// Template group (always visible)
// ---------------------------------------------------------------------------

const saveTemplate: ContextMenuItemDefinition = {
  kind: "action",
  id: "save-template",
  icon: LayoutTemplate,
  label: "Сохранить как шаблон",
  onSelect: (ctx) => ctx.onSaveAsTemplate(),
};

const openTemplates: ContextMenuItemDefinition = {
  kind: "action",
  id: "open-templates",
  icon: FolderOpen,
  label: "Открыть шаблоны",
  onSelect: (ctx) => ctx.onOpenTemplates(),
};

// ---------------------------------------------------------------------------
// Registry map
// ---------------------------------------------------------------------------

export const registry: Record<string, ContextMenuItemDefinition> = {
  spoiler,
  "add-link": addLink,
  "change-url": changeUrl,
  "remove-link": removeLink,
  "insert-table": insertTable,
  "delete-row": deleteRow,
  "delete-column": deleteColumn,
  "delete-table": deleteTable,
  generate,
  placeholder,
  "fill-placeholders": fillPlaceholders,
  "save-template": saveTemplate,
  "open-templates": openTemplates,
};
