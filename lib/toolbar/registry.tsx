/**
 * Toolbar item registry.
 *
 * Every button / control that can appear in any toolbar variant is declared
 * here exactly once. Order is determined by presets, not by this file.
 *
 * Custom items use render-props so their business logic stays co-located
 * with the declaration rather than scattered across rendering components.
 */

import React from "react";
import { ChanceControls } from "@/components/toolbar/chance-controls";
import {
  Bold,
  Italic,
  Strikethrough,
  Code,
  Heading1,
  Heading2,
  Heading3,
  Heading4,
  Heading5,
  Heading6,
  Quote,
  List,
  ListOrdered,
  FileCode,
  PlusSquare,
  Link,
  ChevronsUpDown,
  Check,
  Trash2,
} from "lucide-react";

import { Toggle } from "@/components/ui/toggle";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";

import { isLinkActive, insertInlineEditor } from "@/lib/editor-utils";
import { cn } from "@/lib/utils";

import type { Role } from "@/types/editor-layout";
import type { ToolbarItemDefinition } from "./types";

// ---------------------------------------------------------------------------
// Marks
// ---------------------------------------------------------------------------

const bold: ToolbarItemDefinition = {
  kind: "mark",
  id: "bold",
  format: "bold",
  icon: Bold,
  label: "Bold",
};

const italic: ToolbarItemDefinition = {
  kind: "mark",
  id: "italic",
  format: "italic",
  icon: Italic,
  label: "Italic",
};

const strikethrough: ToolbarItemDefinition = {
  kind: "mark",
  id: "strikethrough",
  format: "strikethrough",
  icon: Strikethrough,
  label: "Strikethrough",
};

const code: ToolbarItemDefinition = {
  kind: "mark",
  id: "code",
  format: "code",
  icon: Code,
  label: "Code",
};

// ---------------------------------------------------------------------------
// Blocks
// ---------------------------------------------------------------------------

const blockquote: ToolbarItemDefinition = {
  kind: "block",
  id: "blockquote",
  format: "blockquote",
  icon: Quote,
  label: "Blockquote",
};

const bulletedList: ToolbarItemDefinition = {
  kind: "block",
  id: "bulleted-list",
  format: "bulleted-list",
  icon: List,
  label: "Bulleted list",
};

const numberedList: ToolbarItemDefinition = {
  kind: "block",
  id: "numbered-list",
  format: "numbered-list",
  icon: ListOrdered,
  label: "Numbered list",
};

const codeBlock: ToolbarItemDefinition = {
  kind: "block",
  id: "code-block",
  format: "code-block",
  icon: FileCode,
  label: "Code block",
};

// ---------------------------------------------------------------------------
// Expandable: heading (H1–H6)
// ---------------------------------------------------------------------------

const heading: ToolbarItemDefinition = {
  kind: "expandable",
  id: "heading",
  label: "Heading",
  icon: Heading1,
  parentFormat: "heading",
  children: [
    {
      id: "heading-1",
      label: "Заголовок 1",
      format: "heading",
      level: 1,
      icon: Heading1,
    },
    {
      id: "heading-2",
      label: "Заголовок 2",
      format: "heading",
      level: 2,
      icon: Heading2,
    },
    {
      id: "heading-3",
      label: "Заголовок 3",
      format: "heading",
      level: 3,
      icon: Heading3,
    },
    {
      id: "heading-4",
      label: "Заголовок 4",
      format: "heading",
      level: 4,
      icon: Heading4,
    },
    {
      id: "heading-5",
      label: "Заголовок 5",
      format: "heading",
      level: 5,
      icon: Heading5,
    },
    {
      id: "heading-6",
      label: "Заголовок 6",
      format: "heading",
      level: 6,
      icon: Heading6,
    },
  ],
};

// ---------------------------------------------------------------------------
// Custom items (render-props)
// ---------------------------------------------------------------------------

/** Link toggle — hidden when showLinkButton is false. */
const link: ToolbarItemDefinition = {
  kind: "custom",
  id: "link",
  render: (ctx) => {
    if (!ctx.showLinkButton) return null;
    const isRawMode = ctx.mode === "raw";
    return React.createElement(
      Toggle,
      {
        size: "sm" as const,
        pressed: !isRawMode && isLinkActive(ctx.editor),
        onPressedChange: ctx.onLinkClick,
        "aria-label": "link",
        onMouseDown: (e: React.MouseEvent) => {
          e.preventDefault();
          e.stopPropagation();
        },
      },
      React.createElement(Link, { className: "h-3 w-3" }),
    );
  },
};

/** Маркдаун / Рендер mode toggle button. */
const modeToggle: ToolbarItemDefinition = {
  kind: "custom",
  id: "mode-toggle",
  render: (ctx) =>
    React.createElement(
      Button,
      {
        type: "button" as const,
        variant: "outline" as const,
        size: "sm" as const,
        onClick: () => ctx.onModeChange(ctx.mode === "raw" ? "parsed" : "raw"),
        onMouseDown: (e: React.MouseEvent) => {
          e.preventDefault();
          e.stopPropagation();
        },
        className: "h-6 gap-1 px-2 text-xs min-w-[86px] bg-transparent",
      },
      ctx.mode === "raw" ? "Маркдаун" : "Рендер",
    ),
};

/**
 * "Спойлер" insert button (main preset only).
 * Conditionally visible via showInlineEditorButton (defaults to true).
 */
const spoiler: ToolbarItemDefinition = {
  kind: "custom",
  id: "spoiler",
  render: (ctx) => {
    if ((ctx.showInlineEditorButton ?? true) === false) return null;
    return React.createElement(
      Button,
      {
        type: "button" as const,
        variant: "outline" as const,
        size: "sm" as const,
        onClick: () => insertInlineEditor(ctx.editor),
        onMouseDown: (e: React.MouseEvent) => {
          e.preventDefault();
          e.stopPropagation();
        },
        className: "h-6 gap-1 px-2 text-xs bg-transparent",
      },
      React.createElement(PlusSquare, { className: "h-3 w-3" }),
      "Спойлер",
    );
  },
};

/**
 * Drag handle wrapper (inline preset only).
 * Imports DragHandle lazily to avoid a hard compile dependency when used
 * in contexts where DragHandle is not available.
 */
const chance: ToolbarItemDefinition = {
  kind: "custom",
  id: "chance",
  render: (ctx) =>
    ctx.chanceConfig
      ? React.createElement(ChanceControls, { config: ctx.chanceConfig })
      : null,
};

const dragHandle: ToolbarItemDefinition = {
  kind: "custom",
  id: "drag-handle",
  render: (_ctx) => {
    // DragHandle is imported at the component level in Toolbar.tsx to keep
    // the registry free of React component imports.  We use a dynamic require
    // here so the registry file does not hard-fail if DragHandle is absent.
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { DragHandle } = require("@/components/editor/drag-handle") as {
      DragHandle: React.ComponentType;
    };
    return React.createElement(
      "div",
      { className: "flex items-center gap-1" },
      React.createElement(DragHandle),
    );
  },
};

// ---------------------------------------------------------------------------
// Internal sub-component: RolesCombobox
// Declared before the `roles` item that references it.
// ---------------------------------------------------------------------------

function RolesCombobox({
  roles: roleList,
  selectedRoles,
  onToggleRole,
}: Readonly<{
  roles: Role[];
  selectedRoles: string[];
  onToggleRole: (id: string) => void;
}>) {
  const [open, setOpen] = React.useState(false);

  const stopMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          size="sm"
          role="combobox"
          aria-expanded={open}
          className="h-6 min-w-35 justify-between text-xs bg-transparent"
          onMouseDown={stopMouseDown}
        >
          {selectedRoles.length === 0
            ? "Выберите роли..."
            : `Всего: ${selectedRoles.length}`}
          <ChevronsUpDown className="ml-1 h-3 w-3 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-55 p-0">
        <Command>
          <CommandInput placeholder="Поиск ролей..." className="h-8" />
          <CommandList>
            <CommandEmpty>Ролей не найдено.</CommandEmpty>
            <CommandGroup>
              {roleList.map((role) => (
                <CommandItem
                  key={role.id}
                  value={role.id.toString()}
                  onSelect={() => onToggleRole(role.id.toString())}
                >
                  <Check
                    className={cn(
                      "mr-2 h-4 w-4",
                      selectedRoles.includes(role.id.toString())
                        ? "opacity-100"
                        : "opacity-0",
                    )}
                  />
                  {role.name}
                </CommandItem>
              ))}
            </CommandGroup>
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}

/**
 * Roles multi-select combobox (inline preset only).
 * Reads roles / selectedRoles / onToggleRole from context.
 */
const roles: ToolbarItemDefinition = {
  kind: "custom",
  id: "roles",
  render: (ctx) => {
    // This item is only meaningful in the inline preset; bail gracefully if
    // the inline-specific context values are absent.
    if (!ctx.roles || !ctx.selectedRoles || !ctx.onToggleRole) return null;

    return React.createElement(RolesCombobox, {
      roles: ctx.roles,
      selectedRoles: ctx.selectedRoles,
      onToggleRole: ctx.onToggleRole,
    });
  },
};

/** Delete button (inline preset only). */
const deleteButton: ToolbarItemDefinition = {
  kind: "custom",
  id: "delete",
  render: (ctx) => {
    if (!ctx.onDelete) return null;
    return React.createElement(
      "div",
      { className: "ml-auto flex items-center gap-2" },
      React.createElement(
        Button,
        {
          type: "button" as const,
          variant: "destructive" as const,
          size: "sm" as const,
          onClick: ctx.onDelete,
          onMouseDown: (e: React.MouseEvent) => {
            e.preventDefault();
            e.stopPropagation();
          },
          className: "h-6 gap-1 px-2 text-xs",
        },
        React.createElement(Trash2, { className: "h-3 w-3" }),
      ),
    );
  },
};

// ---------------------------------------------------------------------------
// Registry map
// ---------------------------------------------------------------------------

export const registry: Record<string, ToolbarItemDefinition> = {
  bold,
  italic,
  strikethrough,
  code,
  chance,
  blockquote,
  "bulleted-list": bulletedList,
  "numbered-list": numberedList,
  "code-block": codeBlock,
  heading,
  link,
  "mode-toggle": modeToggle,
  spoiler,
  "drag-handle": dragHandle,
  roles,
  delete: deleteButton,
};
