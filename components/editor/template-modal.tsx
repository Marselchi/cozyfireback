"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { LayoutTemplate, Search, PencilLine, Trash2 } from "lucide-react";
import { createEditor, type Descendant } from "slate";
import { Slate, Editable, withReact } from "slate-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import useDebounce from "@/hooks/use-debounce";
import { parseMarkdownToSlate } from "@/lib/editor-utils";
import { ConfirmDialog } from "../utils/confirm-dialog";
import {
  useTemplatesList,
  useTemplateContent,
  useRenameTemplate,
  useDeleteTemplate,
} from "@/hooks/use-templates-query";
import type { EditorTemplate } from "@/types/editor-templates";
import { ElementRenderer } from "./element-renderer";
import { LeafRenderer } from "./leaf-renderer";

// ---------------------------------------------------------------------------
// Read-only preview editor
// ---------------------------------------------------------------------------

function TemplatePreview({ markdown }: { markdown: string }) {
  const editor = useMemo(() => withReact(createEditor()), [markdown]);
  const content = useMemo(() => parseMarkdownToSlate(markdown), [markdown]);

  const renderElement = useCallback(
    (props: any) => <ElementRenderer {...props} mode="parsed" roles={[]} />,
    [],
  );

  const renderLeaf = useCallback(
    (props: any) => <LeafRenderer {...props} />,
    [],
  );

  return (
    <Slate editor={editor} initialValue={content}>
      <Editable
        readOnly
        renderElement={renderElement}
        renderLeaf={renderLeaf}
        className="text-lg leading-relaxed pointer-events-none select-none"
      />
    </Slate>
  );
}

type TemplateModalProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onApply: (content: Descendant[]) => void;
};

export function TemplateModal({
  open,
  onOpenChange,
  onApply,
}: Readonly<TemplateModalProps>) {
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const observerTarget = useRef<HTMLDivElement>(null);

  const [renameOpen, setRenameOpen] = useState(false);
  const [renameValue, setRenameValue] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<EditorTemplate | null>(null);

  const debouncedQuery = useDebounce(searchQuery, 300);

  // Reset debounced query on open
  useEffect(() => {
    if (open) {
      setSearchQuery("");
      setSelectedId(null);
      setRenameOpen(false);
      setRenameValue("");
      setDeleteTarget(null);
    }
  }, [open]);

  // Infinite query for templates list (without content) — only fetch when modal is open
  const templatesQuery = useTemplatesList(debouncedQuery, open);

  // Flatten all pages
  const allTemplates = useMemo(
    () => templatesQuery.data?.pages.flatMap((page) => page.content) ?? [],
    [templatesQuery.data],
  );

  // Query for selected template content (loaded on demand)
  const contentQuery = useTemplateContent(selectedId);

  const selectedTemplate = useMemo(
    () => allTemplates.find((t) => t.id === selectedId) ?? null,
    [allTemplates, selectedId],
  );

  // Mutations
  const renameMutation = useRenameTemplate();
  const deleteMutation = useDeleteTemplate();

  // Intersection observer for infinite scroll
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (
          entries[0].isIntersecting &&
          templatesQuery.hasNextPage &&
          !templatesQuery.isFetchingNextPage
        ) {
          templatesQuery.fetchNextPage();
        }
      },
      { threshold: 1 },
    );

    if (observerTarget.current) {
      observer.observe(observerTarget.current);
    }

    return () => observer.disconnect();
  }, [templatesQuery]);

  const handleApply = () => {
    if (!contentQuery.data?.markdown) return;
    const content = parseMarkdownToSlate(contentQuery.data.markdown);
    onApply(content);
    onOpenChange(false);
  };

  const handleCancel = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    onOpenChange(false);
  };

  const handleOpenRename = () => {
    if (!selectedTemplate) return;
    setRenameValue(selectedTemplate.name);
    setRenameOpen(true);
  };

  const handleRenameConfirm = () => {
    const newName = renameValue.trim();
    if (!selectedTemplate || !newName) return;

    renameMutation.mutate(
      { templateId: selectedTemplate.id, newName },
      {
        onSuccess: () => {
          setRenameOpen(false);
        },
      },
    );
  };

  const handleDeleteConfirm = () => {
    if (!deleteTarget) return;

    deleteMutation.mutate(deleteTarget.id, {
      onSuccess: () => {
        setSelectedId((prev) => (prev === deleteTarget.id ? null : prev));
        setDeleteTarget(null);
      },
    });
  };

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange} modal>
        <DialogContent className="flex max-h-[85vh] max-w-3xl flex-col overflow-hidden">
          <DialogDescription />
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <LayoutTemplate className="h-5 w-5" />
              Шаблоны
            </DialogTitle>
          </DialogHeader>

          <div className="relative shrink-0">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Поиск шаблонов..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
              autoFocus
            />
          </div>

          <div className="flex min-h-0 flex-1 gap-3 overflow-hidden">
            {/* Template list */}
            <div className="flex w-56 shrink-0 flex-col overflow-hidden rounded-lg border">
              <div className="flex-1 overflow-y-auto">
                {templatesQuery.isLoading && (
                  <div className="p-4 text-center text-sm text-muted-foreground">
                    Загрузка...
                  </div>
                )}

                {!templatesQuery.isLoading && allTemplates.length === 0 && (
                  <div className="p-4 text-center text-sm text-muted-foreground">
                    Ничего нет
                  </div>
                )}

                {!templatesQuery.isLoading &&
                  allTemplates.map((tpl) => (
                    <button
                      key={tpl.id}
                      type="button"
                      onClick={() => setSelectedId(tpl.id)}
                      className={cn(
                        "w-full px-4 py-3 text-left transition-colors hover:bg-accent text-sm",
                        selectedId === tpl.id &&
                          "bg-accent border-l-4 border-primary",
                      )}
                    >
                      <div className="font-medium text-foreground truncate">
                        {tpl.name}
                      </div>
                    </button>
                  ))}

                {templatesQuery.hasNextPage && (
                  <div
                    ref={observerTarget}
                    className="p-3 text-center text-sm text-muted-foreground"
                  >
                    {templatesQuery.isFetchingNextPage
                      ? "Загрузка..."
                      : "Загрузить еще"}
                  </div>
                )}
              </div>
            </div>

            {/* Preview panel */}
            <div className="flex-1 overflow-y-auto rounded-lg border">
              {selectedId !== null && contentQuery.isLoading ? (
                <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
                  Загрузка шаблона...
                </div>
              ) : selectedTemplate && contentQuery.data?.markdown ? (
                <div className="p-4">
                  <div className="mb-3 flex items-start justify-between gap-3">
                    <div>
                      <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        Предпросмотр
                      </p>
                      <p className="text-sm font-semibold text-foreground">
                        {selectedTemplate.name}
                      </p>
                    </div>

                    <div className="flex shrink-0 gap-2">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={handleOpenRename}
                      >
                        <PencilLine className="mr-2 h-4 w-4" />
                        Переименовать
                      </Button>
                      <Button
                        type="button"
                        variant="destructive"
                        size="sm"
                        onClick={() => setDeleteTarget(selectedTemplate)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>

                  <TemplatePreview
                    key={selectedTemplate.id}
                    markdown={contentQuery.data.markdown}
                  />
                </div>
              ) : (
                <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
                  Выберите шаблон для предпросмотра
                </div>
              )}
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={handleCancel}>
              Отмена
            </Button>
            <Button
              type="button"
              disabled={!contentQuery.data}
              onClick={handleApply}
            >
              Применить шаблон
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {renameOpen && (
        <Dialog open={renameOpen} onOpenChange={setRenameOpen} modal>
          <DialogContent className="max-w-md">
            <DialogHeader>
              <DialogTitle>Переименовать шаблон</DialogTitle>
              <DialogDescription>
                Введите новое название шаблона.
              </DialogDescription>
            </DialogHeader>

            <Input
              value={renameValue}
              onChange={(e) => setRenameValue(e.target.value)}
              placeholder="Название шаблона"
              autoFocus
              onKeyDown={(e) => {
                if (e.key === "Enter") handleRenameConfirm();
              }}
            />

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => setRenameOpen(false)}
              >
                Отмена
              </Button>
              <Button
                type="button"
                onClick={handleRenameConfirm}
                disabled={!renameValue.trim()}
              >
                Сохранить
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null);
        }}
        title="Удалить шаблон?"
        description={`Шаблон «${deleteTarget?.name ?? ""}» будет удалён без возможности восстановления.`}
        confirmButtonType="delete"
        onAccept={handleDeleteConfirm}
      />
    </>
  );
}
