"use client";

import { useCallback, useMemo, useEffect } from "react";
import { Pencil, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { TextContent } from "@/components/lore/text-content";
import { useParams } from "next/navigation";
import { Excerpt } from "@/types/lore";
import { EditContext } from "@/types/characters";
import type { EditorContent } from "@/types/editor";
import type { Role } from "@/types/editor-layout";
import {
  parseMarkdownToSlate,
  serializeToMarkdown,
  serializeToMarkdownPreview,
} from "@/lib/editor-utils";
import {
  EditorStateProvider,
  useEditorState,
} from "@/components/editor/editor-state-provider";
import { SlateEditor } from "@/components/editor/slate-editor";
import { useShallow } from "zustand/react/shallow";

interface CharacterTextBlock {
  label: string;
  content: string;
  roles?: number[];
}

interface TextBlockProps {
  block: CharacterTextBlock;
  editContext: EditContext;
  onStartEdit: () => void;
  onSave: (content: string) => void;
  onCancelEdit: () => void;
  excerpts?: Excerpt[];
  canEdit?: boolean;
  roles?: Role[];
}

export function TextBlock({
  block,
  editContext,
  onStartEdit,
  onSave,
  onCancelEdit,
  excerpts,
  canEdit = true,
  roles = [],
}: TextBlockProps) {
  const isEditing =
    editContext.blockId === "textBlock" && editContext.field === "content";

  // Конвертация markdown в Slate формат
  const initialContent: EditorContent = useMemo(
    () => ({
      content: parseMarkdownToSlate(block.content || ""),
      roles: roles,
    }),
    [block.content, block.roles, roles],
  );

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <h3 className="text-lg font-semibold">{block.label}</h3>
      </div>

      <div>
        {isEditing ? (
          <EditorStateProvider>
            <SlateEditorWithSave
              initialContent={initialContent}
              dbContent={block.content}
              onSave={onSave}
              onCancel={onCancelEdit}
            />
          </EditorStateProvider>
        ) : (
          <TextBlockPreview
            content={block.content}
            excerpts={excerpts}
            onStartEdit={canEdit ? onStartEdit : undefined}
            canEdit={canEdit}
          />
        )}
      </div>
    </div>
  );
}

interface TextBlockPreviewProps {
  content: string;
  excerpts?: Excerpt[];
  onStartEdit?: () => void;
  canEdit?: boolean;
}

function TextBlockPreview({
  content,
  excerpts,
  onStartEdit,
  canEdit = true,
}: TextBlockPreviewProps) {
  // Отображаем previewValue через serializeToMarkdownPreview
  const previewMarkdown = useMemo(() => {
    const slateContent = parseMarkdownToSlate(content || "");
    return serializeToMarkdownPreview(slateContent);
  }, [content]);

  return (
    <div className="relative group">
      <TextContent
        content={previewMarkdown}
        mdOptions={{ anchorClickable: false, showExcerpts: true }}
      />
      {canEdit && onStartEdit && (
        <Button
          onClick={onStartEdit}
          className={`absolute top-0 right-0 ${content ? "opacity-0" : ""} group-hover:opacity-100 transition-opacity shrink-0`}
          title="Редактировать"
          variant={"outline"}
          size="sm"
        >
          {content ? (
            <Pencil className="w-4 h-4" />
          ) : (
            <Plus className="w-4 h-4" />
          )}
        </Button>
      )}
    </div>
  );
}

// Обертка над SlateEditor с кнопками Save/Cancel
interface SlateEditorWithSaveProps {
  initialContent: EditorContent;
  dbContent: string;
  onSave: (content: string) => void;
  onCancel: () => void;
}

function SlateEditorWithSave({
  initialContent,
  dbContent,
  onSave,
  onCancel,
}: SlateEditorWithSaveProps) {
  const { previewValue, resetPreviewStore } = useEditorState(
    useShallow((s) => ({
      previewValue: s.previewValue,
      resetPreviewStore: s.resetPreviewStore,
    })),
  );

  // При монтировании загружаем оригинальное значение с маркерами БД
  useEffect(() => {
    if (dbContent) {
      const slateWithDbMarkers = parseMarkdownToSlate(dbContent);
      resetPreviewStore(slateWithDbMarkers);
    }
  }, [dbContent, resetPreviewStore]);

  // Конвертируем preview в markdown при сохранении
  const handleSave = useCallback(() => {
    if (previewValue) {
      const markdown = serializeToMarkdown(previewValue);
      onSave(markdown);
    }
  }, [previewValue, onSave]);

  return (
    <div className="space-y-2">
      <SlateEditor initialContent={initialContent} />
      <div className="flex items-center justify-end gap-2">
        <Button variant="outline" size="sm" onClick={onCancel}>
          Отмена
        </Button>
        <Button size="sm" onClick={handleSave}>
          Сохранить
        </Button>
      </div>
    </div>
  );
}
