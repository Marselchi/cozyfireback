"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Calendar, FileEdit, Trash2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import CustomLink from "@/components/no-prefetch-link";
import { Draft, deleteDraft } from "../../../lib/utils/draft-utils";
import { useParams } from "next/navigation";
import { useRoomId } from "@/lib/room-utils";

interface DraftsListProps {
  drafts: Draft[];
}

export default function DraftsList({
  drafts: initialDrafts,
}: Readonly<DraftsListProps>) {
  const [drafts, setDrafts] = useState<Draft[]>(initialDrafts);
  const roomName = useRoomId();

  // Update local drafts when initial drafts prop changes
  useEffect(() => {
    setDrafts(initialDrafts);
  }, [initialDrafts]);
  if (drafts.length === 0) {
    return (
      <div className="text-center py-12">
        <FileEdit className="mx-auto h-12 w-12 text-muted-foreground/50 mb-4" />
        <h3 className="text-xl font-medium">Нет черновиков</h3>
        <p className="text-muted-foreground mt-2">
          Ваши черновики появятся здесь
        </p>
      </div>
    );
  }

  return (
    <div className="min-h-full">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
        {drafts.map((draft) => {
          const formattedDate = new Date(draft.updatedAt).toLocaleDateString(
            "ru-RU",
            {
              year: "numeric",
              month: "long",
              day: "numeric",
              hour: "2-digit",
              minute: "2-digit",
            },
          );

          return (
            <Card
              key={draft.id}
              className="group relative h-full cursor-pointer overflow-hidden border border-amber-400/60 bg-card transition-all duration-300 hover:border-amber-400 hover:shadow-md"
            >
              <div className="pointer-events-none absolute inset-x-0 top-0 h-1 bg-linear-to-r from-amber-400 via-amber-300 to-amber-400 opacity-70 transition-opacity duration-300 group-hover:opacity-100" />

              <CustomLink
                href={`./lore/drafts/${draft.id}`}
                className="block h-full"
              >
                <CardHeader className="pb-3">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <CardTitle className="truncate text-base sm:text-lg font-semibold tracking-tight">
                        {draft.metadata.name}
                      </CardTitle>
                      <div className="mt-1 flex items-center gap-1.5 text-xs text-muted-foreground">
                        <Calendar className="h-3.5 w-3.5" aria-hidden="true" />
                        <span className="truncate">{formattedDate}</span>
                      </div>
                    </div>

                    <div className="flex items-center gap-1">
                      <Badge
                        variant="secondary"
                        className="shrink-0 gap-1 rounded-full border border-amber-400/80 bg-amber-400/20 text-foreground"
                        aria-label="Черновик"
                      >
                        <span className="text-[10px] uppercase tracking-wide">
                          {draft.forId ? "Редактирование" : "Создание"}
                        </span>
                      </Badge>
                      <button
                        onClick={(e) => {
                          e.preventDefault();
                          if (
                            confirm(
                              "Вы уверены, что хотите удалить этот черновик?",
                            )
                          ) {
                            deleteDraft(roomName, draft.id);
                            // Update local state to remove the deleted draft
                            setDrafts((prevDrafts) =>
                              prevDrafts.filter((d) => d.id !== draft.id),
                            );
                          }
                        }}
                        className="p-1.5 rounded-full hover:bg-destructive/10 text-destructive hover:text-destructive/90 transition-colors"
                        aria-label="Удалить черновик"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                </CardHeader>

                {draft.metadata.description && (
                  <CardContent className="pb-4">
                    <p className="text-sm text-muted-foreground line-clamp-2">
                      {draft.metadata.description}
                    </p>
                  </CardContent>
                )}
              </CustomLink>
            </Card>
          );
        })}
      </div>

      <div className="text-center text-sm text-muted-foreground mt-4">
        Всего черновиков: {drafts.length}
      </div>
    </div>
  );
}
