"use client";

import type React from "react";

import { useEffect, useMemo, useState } from "react";
import { LinkIcon, Search } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "next/navigation";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

import useDebounce from "@/hooks/use-debounce";
import type { LoreEntry } from "@/types/questions";
import { getPaginatedLore } from "@/server/lore/lore";
import { useRoomId } from "@/lib/room-utils";

export type LinkModalData = {
  name: string;
  url: string;
  isEditing?: boolean;
};

type LinkModalProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initialData?: LinkModalData;
  onSubmit: (data: LinkModalData) => void;
};
function buildLoreUrl(roomName: string, loreId: string | number) {
  return `https://${process.env.NEXT_PUBLIC_DOMAIN}/rooms/${roomName}/lore/${loreId}`;
}

export function LinkModal({
  open,
  onOpenChange,
  initialData,
  onSubmit,
}: Readonly<LinkModalProps>) {
  const { roomName } = useParams<{ roomName: string }>();
  const roomId = useRoomId();

  const isEditing = !!initialData?.isEditing;

  const [name, setName] = useState(initialData?.name ?? "");
  const [url, setUrl] = useState(initialData?.url ?? "");

  const [searchQuery, setSearchQuery] = useState("");
  const [selectedLore, setSelectedLore] = useState<LoreEntry | null>(null);

  const debouncedQuery = useDebounce(searchQuery, 300);

  const loreQuery = useQuery({
    queryKey: ["lore-search", roomId, debouncedQuery],
    queryFn: () =>
      getPaginatedLore(
        roomId,
        {
          title: debouncedQuery || undefined, // передаём как фильтр
        },
        {
          size: 5,
        },
      ),
    enabled: open && debouncedQuery.trim().length > 0,
    staleTime: 5 * 60 * 1000,
  });

  const loreEntries = loreQuery.data?.content ?? [];

  // При открытии модалки и при смене initialData — инициализируем поля
  useEffect(() => {
    if (!open) return;
    setName(initialData?.name ?? "");
    setUrl(initialData?.url ?? "");
    setSearchQuery("");
    setSelectedLore(null);
  }, [open, initialData?.name, initialData?.url, initialData?.isEditing]);

  const title = useMemo(() => {
    if (isEditing) return "Изменение ссылки";
    return "Вставить ссылку";
  }, [isEditing]);

  const handleOpenChange = (newOpen: boolean) => {
    if (!newOpen) {
      setSearchQuery("");
      setSelectedLore(null);
      setName("");
      setUrl("");
    }
    onOpenChange(newOpen);
  };

  const handleCancel = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    handleOpenChange(false);
  };

  const handleLoreSelect = (lore: LoreEntry) => {
    setSelectedLore(lore);

    const nextUrl = buildLoreUrl(roomName, lore.id);
    setUrl(nextUrl);

    // При вставке — имя по умолчанию из заголовка (но пользователь может поменять)
    if (!isEditing) {
      setName((prev) => (prev.trim().length ? prev : lore.title));
    }
    // При редактировании имя НЕ трогаем вообще
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    e.stopPropagation();

    const finalUrl = url.trim();
    if (!finalUrl) return;

    const finalName = isEditing
      ? (initialData?.name ?? "").trim()
      : name.trim() || finalUrl;

    onSubmit({
      name: finalName,
      url: finalUrl,
      isEditing,
    });

    handleOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange} modal={true}>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <LinkIcon className="h-5 w-5" />
            <span>{title}</span>
          </DialogTitle>
        </DialogHeader>

        <form
          onSubmit={handleSubmit}
          className="flex-1 overflow-hidden flex flex-col gap-4"
        >
          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Поиск по заголовку..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>

          {/* List */}
          <div className="flex-1 overflow-y-auto rounded-lg border">
            {loreQuery.isFetching && (
              <div className="p-4 text-center">Поиск...</div>
            )}

            {loreQuery.isError && (
              <div className="p-4 text-center text-destructive">Ошибочка</div>
            )}

            {debouncedQuery.trim().length > 0 &&
              loreEntries.length === 0 &&
              !loreQuery.isFetching && (
                <div className="p-4 text-center text-muted-foreground">
                  Ничего нет
                </div>
              )}

            {loreEntries.length > 0 && (
              <div>
                {loreEntries.map((lore) => {
                  const active = selectedLore?.id === lore.id;
                  return (
                    <button
                      key={lore.id}
                      type="button"
                      onClick={() => handleLoreSelect(lore)}
                      className={cn(
                        "w-full p-4 text-left transition-colors hover:bg-accent",
                        active && "bg-accent border-l-4 border-blue-500",
                      )}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0 flex-1">
                          <div className="font-medium text-primary mb-1 truncate">
                            {lore.title}
                          </div>
                          {lore.description && (
                            <div className="text-sm text-muted-foreground mb-2 line-clamp-2">
                              {lore.description}
                            </div>
                          )}
                          <div className="text-xs text-muted-foreground truncate">
                            {buildLoreUrl(roomName, lore.id)}
                          </div>
                        </div>
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          <div className="grid gap-4 rounded-lg bg-muted/40 p-4">
            <div className="grid gap-2">
              <Label htmlFor="name">Отображаемое имя</Label>
              <Input
                id="name"
                value={isEditing ? (initialData?.name ?? "") : name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Введите отображаемое имя"
                disabled={isEditing}
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="url">URL</Label>
              <Input
                id="url"
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                placeholder={`/${roomName}/lore/123`}
                required
              />
              <p className="text-xs text-muted-foreground">
                {isEditing
                  ? "Выбери другой лор в списке — обновится только URL."
                  : "Выбери лор в списке — URL подставится автоматически."}
              </p>
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={handleCancel}>
              Отмена
            </Button>

            <Button type="submit" disabled={!url.trim()}>
              {isEditing ? "Обновить" : "Вставить"} ссылку
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
