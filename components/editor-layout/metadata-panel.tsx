"use client";

import { useState, useMemo, useTransition, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  ChevronRight,
  ChevronLeft,
  X,
  Search,
  ChevronDown,
  Eye,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { ScrollArea, ScrollBar } from "@/components/ui/scroll-area";
import { cn } from "@/lib/utils";
import type { Tag, Role, DocumentMetadata } from "@/types/editor-layout";
import { createLoreItem, updateLoreItem } from "@/server/lore/lore";
import {
  useEditorState,
  useEditorStateApi,
} from "../editor/editor-state-provider";
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
} from "@radix-ui/react-dropdown-menu";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useBlockLayout } from "./block-layout-context";
import { showToast } from "../utils/toast-utils";
import { useRoomId } from "@/lib/room-utils";
import { useQueryClient } from "@tanstack/react-query";

interface MetadataPanelProps {
  initialMetadata?: DocumentMetadata;
  tags: Tag[];
  roles: Role[];
  id?: number;
}

function emptyMeta(): DocumentMetadata {
  return {
    name: "Без названия",
    description: "",
    date: "",
    selectedTagsId: [],
    selectedRolesId: [],
  };
}

function isMeaningfulMetadata(meta?: DocumentMetadata) {
  if (!meta) return false;

  const isDefaultName =
    meta.name.trim() === "" || meta.name.trim() === "Без названия";

  return (
    !isDefaultName ||
    meta.description.trim() !== "" ||
    meta.date.trim() !== "" ||
    meta.selectedTagsId.length > 0 ||
    meta.selectedRolesId.length > 0
  );
}

function sameMetadata(a: DocumentMetadata, b: DocumentMetadata) {
  return (
    a.name === b.name &&
    a.description === b.description &&
    a.date === b.date &&
    a.selectedTagsId.length === b.selectedTagsId.length &&
    a.selectedRolesId.length === b.selectedRolesId.length &&
    a.selectedTagsId.every((v, i) => v === b.selectedTagsId[i]) &&
    a.selectedRolesId.every((v, i) => v === b.selectedRolesId[i])
  );
}

export function MetadataPanel({
  initialMetadata,
  tags,
  roles,
  id,
}: Readonly<MetadataPanelProps>) {
  const roomName = useRoomId();
  const router = useRouter();
  const queryClient = useQueryClient();

  // Only currentDraftMetadata is rendered (drives the effect below that
  // syncs into local `metadata` state) — so it's the only reactive
  // subscription. Everything else here is read only inside handlers/effects,
  // never in JSX, so it's pulled imperatively via editorStateApi.getState()
  // at the point of use instead — that way edits to previewValue (which
  // happen on every keystroke) don't re-render this panel at all.
  const currentDraftMetadata = useEditorState((s) => s.currentDraftMetadata);
  const editorStateApi = useEditorStateApi();

  const { addBlock, canAddAtPosition, hasPreviewBlock } = useBlockLayout();

  const [isOpen, setIsOpen] = useState(false);
  const [tagSearch, setTagSearch] = useState("");
  const [roleSearch, setRoleSearch] = useState("");
  const [isPending, startTransition] = useTransition();
  const [previewMenuOpen, setPreviewMenuOpen] = useState(false);
  const [selectedPosition, setSelectedPosition] = useState<
    "top" | "bottom" | "left" | "right" | null
  >(null);

  const [metadata, setMetadata] = useState<DocumentMetadata>(() => {
    if (isMeaningfulMetadata(currentDraftMetadata)) {
      return currentDraftMetadata;
    }

    if (initialMetadata && isMeaningfulMetadata(initialMetadata)) {
      return initialMetadata;
    }

    return emptyMeta();
  });

  const handlePositionSelect = (
    position: "top" | "bottom" | "left" | "right",
  ) => {
    setSelectedPosition(position);
  };

  const handleAddPreview = () => {
    if (selectedPosition) {
      addBlock("preview", selectedPosition, "editor-initial");
    }
    setPreviewMenuOpen(false);
    setSelectedPosition(null);
  };

  const handlePreviewMenuOpenChange = (open: boolean) => {
    setPreviewMenuOpen(open);
    if (!open) {
      setSelectedPosition(null);
    }
  };

  const canAddTop = canAddAtPosition("top");
  const canAddBottom = canAddAtPosition("bottom");
  const canAddLeft = canAddAtPosition("left");
  const canAddRight = canAddAtPosition("right");

  useEffect(() => {
    const source = isMeaningfulMetadata(currentDraftMetadata)
      ? currentDraftMetadata
      : isMeaningfulMetadata(initialMetadata)
        ? initialMetadata
        : null;

    if (!source) return;

    setMetadata((prev) => (sameMetadata(prev, source) ? prev : source));
  }, [currentDraftMetadata, initialMetadata, id]);

  useEffect(() => {
    const save = () => {
      editorStateApi.getState().updateDraftMetadata({
        name: metadata.name || "Без названия",
        description: metadata.description || "",
        date: metadata.date || "",
        selectedTagsId: metadata.selectedTagsId || [],
        selectedRolesId: metadata.selectedRolesId || [],
      });
    };

    const metadataTimer = setInterval(save, 5000);

    return () => {
      clearInterval(metadataTimer);
    };
  }, [metadata, editorStateApi]);

  const filteredTags = useMemo(() => {
    if (!tagSearch.trim()) return tags;
    return tags.filter((tag) =>
      tag.name.toLowerCase().includes(tagSearch.toLowerCase()),
    );
  }, [tags, tagSearch]);

  const filteredRoles = useMemo(() => {
    if (!roleSearch.trim()) return roles;
    return roles.filter((role) =>
      role.name.toLowerCase().includes(roleSearch.toLowerCase()),
    );
  }, [roles, roleSearch]);

  const toggleTag = (tagId: number) => {
    setMetadata((prev) => ({
      ...prev,
      selectedTagsId: prev.selectedTagsId.includes(tagId)
        ? prev.selectedTagsId.filter((id) => id !== tagId)
        : [...prev.selectedTagsId, tagId],
    }));
  };

  const toggleRole = (roleId: number) => {
    setMetadata((prev) => ({
      ...prev,
      selectedRolesId: prev.selectedRolesId.includes(roleId)
        ? prev.selectedRolesId.filter((id) => id !== roleId)
        : [...prev.selectedRolesId, roleId],
    }));
  };

  const handleSaveDraft = () => {
    const nextMetadata = {
      ...metadata,
      name:
        metadata.name.length === 0
          ? "Черновик " + new Date().toLocaleString()
          : metadata.name,
    };

    editorStateApi.getState().forceSaveWithMetadata(nextMetadata);
    router.push(`/rooms/${roomName}/lore/${id ?? ""}`);
  };

  const handleSave = () => {
    startTransition(async () => {
      const {
        previewValue,
        getInlineBlocksDiff,
        deleteCurrentDraft,
        resetInlineBlockTracking,
      } = editorStateApi.getState();
      const inlineBlocks = getInlineBlocksDiff(previewValue);

      if (id) {
        const updatePromise = updateLoreItem({
          id,
          roomName,
          metadata,
          content: previewValue,
          inlineBlocks,
        });
        showToast({
          title: "Обновление статьи",
          description: "Статья обновляется.",
          type: "promise",
          promise: updatePromise,
          loading: "Обновление...",
          success: "Статья успешно обновлена!",
          error: "Ошибка обновления статьи, попробуйте снова",
        });
        try {
          await updatePromise;
          resetInlineBlockTracking();
          deleteCurrentDraft();
          queryClient.invalidateQueries({ queryKey: ["lore", roomName, id] });
          queryClient.invalidateQueries({ queryKey: ["lore-list", roomName] });
          router.push(`/rooms/${roomName}/lore/${id ?? ""}`);
        } catch (err) {
          console.error("Save failed:", err);
        }
      } else {
        const createPromise = createLoreItem({
          roomName,
          metadata,
          content: previewValue,
          inlineBlocks,
        });
        showToast({
          title: "Сохранение статьи",
          description: "Статья сохраняется.",
          type: "promise",
          promise: createPromise,
          loading: "Сохраняем...",
          success: "Статья успешно сохранена!",
          error: "Ошибка сохранения статьи, попробуйте снова",
        });
        try {
          await createPromise;
          resetInlineBlockTracking();
          deleteCurrentDraft();
          queryClient.invalidateQueries({ queryKey: ["lore-list", roomName] });
          router.push(`/rooms/${roomName}/lore/${id ?? ""}`);
        } catch (err) {
          console.error("Save failed:", err);
        }
      }
    });
  };

  const handleCancel = () => {
    editorStateApi.getState().resetPreviewStore();
    router.push(`/rooms/${roomName}/lore/${id ?? ""}`);
  };

  return (
    <>
      {!isOpen && !hasPreviewBlock() && (
        <Popover
          open={previewMenuOpen}
          onOpenChange={handlePreviewMenuOpenChange}
        >
          <PopoverTrigger asChild>
            <button className="fixed right-0 top-[calc(50%+80px)] z-40 flex h-10 w-6 -translate-y-1/2 items-center justify-center rounded-l-md bg-primary/80 text-primary-foreground shadow-lg transition-colors hover:bg-primary/90">
              <Eye className="h-4 w-4" />
            </button>
          </PopoverTrigger>
          <PopoverContent className="w-48 p-2" align="end" side="left">
            {selectedPosition === null ? (
              <div className="space-y-1">
                <p className="mb-2 px-2 text-xs font-medium text-muted-foreground">
                  Куда добавить превью
                </p>
                <button
                  onClick={() => handlePositionSelect("top")}
                  disabled={!canAddTop}
                  className="flex w-full items-center gap-2 rounded-md px-2 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Сверху
                </button>
                <button
                  onClick={() => handlePositionSelect("bottom")}
                  disabled={!canAddBottom}
                  className="flex w-full items-center gap-2 rounded-md px-2 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Снизу
                </button>
                <button
                  onClick={() => handlePositionSelect("left")}
                  disabled={!canAddLeft}
                  className="flex w-full items-center gap-2 rounded-md px-2 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Слева
                </button>
                <button
                  onClick={() => handlePositionSelect("right")}
                  disabled={!canAddRight}
                  className="flex w-full items-center gap-2 rounded-md px-2 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Справа
                </button>
              </div>
            ) : (
              <div className="space-y-1">
                <button
                  onClick={() => setSelectedPosition(null)}
                  className="mb-2 flex items-center gap-1 px-2 text-xs font-medium text-muted-foreground hover:text-foreground"
                >
                  ← Назад
                </button>
                <p className="mb-2 px-2 text-xs font-medium text-muted-foreground">
                  Добавить превью?
                </p>
                <button
                  onClick={handleAddPreview}
                  className="flex w-full items-center gap-2 rounded-md bg-primary px-2 py-2 text-sm text-primary-foreground hover:bg-primary/90"
                >
                  Добавить
                </button>
              </div>
            )}
          </PopoverContent>
        </Popover>
      )}

      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          className="fixed right-0 top-1/2 z-40 flex h-24 w-6 -translate-y-1/2 items-center justify-center rounded-l-md bg-primary/80 text-primary-foreground shadow-lg transition-colors hover:bg-primary/90"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
      )}

      <div
        className={cn(
          "fixed right-0 top-0 z-50 h-full w-80 transform border-l border-border bg-card shadow-xl transition-transform duration-300",
          isOpen ? "translate-x-0" : "translate-x-full",
        )}
      >
        <div className="flex h-full flex-col">
          <div className="flex shrink-0 items-center justify-between border-b border-border px-4 py-3">
            <h2 className="font-semibold text-card-foreground">Метаданные</h2>
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setIsOpen(false)}
              className="h-8 w-8"
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>

          <ScrollArea className="flex-1 max-h-[calc(100vh-130px)]">
            <div className="space-y-6 p-4">
              <div className="space-y-2">
                <Label htmlFor="name">Заголовок</Label>
                <Input
                  id="name"
                  value={metadata.name}
                  onChange={(e) =>
                    setMetadata((prev) => ({ ...prev, name: e.target.value }))
                  }
                  placeholder="Заголовок"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="description">*Описание</Label>
                <Textarea
                  id="description"
                  value={metadata.description}
                  onChange={(e) =>
                    setMetadata((prev) => ({
                      ...prev,
                      description: e.target.value,
                    }))
                  }
                  placeholder="Опционально"
                  rows={3}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="date">Дата</Label>
                <Input
                  id="date"
                  type="text"
                  value={metadata.date}
                  onChange={(e) =>
                    setMetadata((prev) => ({ ...prev, date: e.target.value }))
                  }
                  placeholder="Можно текстом"
                />
              </div>

              <div className="space-y-2">
                <Label>Теги</Label>
                <div className="relative">
                  <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                  <Input
                    placeholder="Поиск тегов..."
                    value={tagSearch}
                    onChange={(e) => setTagSearch(e.target.value)}
                    className="pl-8"
                  />
                </div>
                <ScrollArea className="h-32 rounded-md border border-border">
                  <div className="flex flex-wrap gap-2 p-2">
                    {filteredTags.map((tag) => {
                      const isSelected = metadata.selectedTagsId.includes(
                        tag.id,
                      );
                      return (
                        <Badge
                          key={tag.id}
                          variant={isSelected ? "default" : "outline"}
                          onClick={() => toggleTag(tag.id)}
                        >
                          {tag.name}
                          {isSelected && <X className="ml-1 h-3 w-3" />}
                        </Badge>
                      );
                    })}
                    {filteredTags.length === 0 && (
                      <p className="p-2 text-sm text-muted-foreground">
                        Тегов не найдено
                      </p>
                    )}
                  </div>
                  <ScrollBar />
                </ScrollArea>
              </div>

              <div className="space-y-2">
                <Label>Роли</Label>
                <div className="relative">
                  <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                  <Input
                    placeholder="Поиск ролей..."
                    value={roleSearch}
                    onChange={(e) => setRoleSearch(e.target.value)}
                    className="pl-8"
                  />
                </div>
                <ScrollArea className="h-60 rounded-md border border-border">
                  <div className="space-y-2 p-2">
                    {filteredRoles.map((role) => {
                      const isSelected = metadata.selectedRolesId.includes(
                        role.id,
                      );
                      return (
                        <div key={role.id} className="space-y-2">
                          <button
                            onClick={() => toggleRole(role.id)}
                            className={cn(
                              "w-full rounded-md border p-3 text-left transition-colors",
                              isSelected
                                ? "border-primary bg-additional"
                                : "border-border hover:bg-accent bg-input/30",
                            )}
                          >
                            <div className="font-medium text-sm">
                              {role.name}
                            </div>

                            {role.users && role.users.length > 0 && (
                              <div className="flex flex-wrap gap-1 pt-2">
                                <span className="text-sm">Владеют:</span>
                                {role.users.map((userName, index) => (
                                  <Badge
                                    key={index + userName}
                                    variant="secondary"
                                    className="text-xs"
                                  >
                                    {userName}
                                  </Badge>
                                ))}
                              </div>
                            )}
                          </button>
                        </div>
                      );
                    })}
                    {filteredRoles.length === 0 && (
                      <p className="p-2 text-sm text-muted-foreground">
                        Ролей не найдено
                      </p>
                    )}
                  </div>
                  <ScrollBar />
                </ScrollArea>
              </div>
            </div>
            <ScrollBar />
          </ScrollArea>

          <div className="shrink-0 border-t border-border p-4">
            <div className="flex gap-2">
              <Button
                variant="outline"
                onClick={handleCancel}
                className="flex-1 bg-transparent"
              >
                Отмена
              </Button>
              <div className="relative flex-1">
                <Button
                  onClick={handleSave}
                  className="w-full pr-10"
                  disabled={isPending}
                >
                  {isPending ? "Сохраняю..." : "Сохранить"}
                </Button>

                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="absolute right-0 top-0 h-full w-10 rounded-l-none hover:bg-accent/20"
                      disabled={isPending}
                      aria-label="Дополнительные опции сохранения"
                    >
                      <ChevronDown className="h-4 w-4 opacity-70 text-accent" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end" className="w-40">
                    <DropdownMenuItem
                      onClick={handleSaveDraft}
                      className="cursor-pointer focus:bg-primary/90 bg-primary text-primary-foreground text-sm p-2 border-input border"
                    >
                      Сохранить черновик
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </div>
          </div>
        </div>
      </div>

      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-background/50 backdrop-blur-sm"
          onClick={() => setIsOpen(false)}
        />
      )}
    </>
  );
}
