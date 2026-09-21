"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { SlateEditorSimple } from "./slate-editor-simple";
import {
  CATEGORIES,
  type QuestionCategory,
  type QuestionTypeName,
} from "@/types/questions";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Descendant } from "slate";
import { serializeToMarkdown } from "@/lib/editor-utils";
import { emptyContent } from "@/types/editor";
import { createQuestion } from "@/server/questions/question";
import { useParams } from "next/navigation";
import { useRoomId } from "@/lib/room-utils";

interface CreateQuestionFormProps {
  loreId?: string;
  characterId?: string;
  onClose: () => void;
  onCreated?: () => void;
}

export function CreateQuestionForm({
  loreId,
  characterId,
  onClose,
  onCreated,
}: Readonly<CreateQuestionFormProps>) {
  const queryClient = useQueryClient();
  const [title, setTitle] = useState("");
  const [body, setBody] = useState<Descendant[]>(emptyContent);
  const [typeName, setTypeName] = useState<QuestionTypeName>(
    loreId ? "Лор" : characterId ? "Персонаж" : "Общие",
  );
  const [category, setCategory] = useState<QuestionCategory | undefined>(
    undefined,
  );

  const roomName = useRoomId();

  const mutation = useMutation({
    mutationFn: createQuestion,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["questions"] });
      onCreated?.();
      onClose();
    },
  });

  const handleSubmit = (e: React.SubmitEvent) => {
    e.preventDefault();
    if (!title.trim() || !body) return;
    mutation.mutate({
      request: {
        title,
        body: serializeToMarkdown(body),
        category: category ?? undefined,
        loreId: typeName === "Лор" ? loreId : undefined,
        characterId: typeName === "Персонаж" ? characterId : undefined,
      },
      roomName: roomName,
    });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4 p-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-foreground">Новый вопрос</h3>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={onClose}
          className="h-7 w-7 p-0"
        >
          <X className="w-4 h-4" />
        </Button>
      </div>

      <div className="space-y-3">
        <Input
          placeholder="Заголовок вопроса..."
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className="text-sm"
        />

        {/* Type selector */}
        <Select
          value={typeName}
          onValueChange={(val) => {
            setTypeName(val as QuestionTypeName);
            if (val === "Лор" || val === "Персонаж") {
              setCategory(undefined);
            }
          }}
        >
          <SelectTrigger className="text-sm h-9">
            <SelectValue placeholder="Тип вопроса" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="Лор">Лор</SelectItem>
            <SelectItem value="Персонаж">Персонаж</SelectItem>
            <SelectItem value="Общие">Общие</SelectItem>
          </SelectContent>
        </Select>

        {/* Category selector: only shown when type is NOT "lore" */}
        {typeName !== "Лор" && typeName !== "Персонаж" && (
          <Select
            value={category?.id ?? ""}
            onValueChange={(val) => {
              const found = CATEGORIES.find((c) => c.id === val);
              setCategory(found);
            }}
          >
            <SelectTrigger className="text-sm h-9">
              <SelectValue placeholder="Категория" />
            </SelectTrigger>
            <SelectContent>
              {CATEGORIES.map((cat) => (
                <SelectItem key={cat.id} value={cat.id}>
                  {cat.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}

        <SlateEditorSimple initialContent={body} onChange={setBody} />
      </div>

      <div className="flex justify-end gap-2">
        <Button type="button" variant="ghost" size="sm" onClick={onClose}>
          Отмена
        </Button>
        <Button
          type="submit"
          size="sm"
          disabled={mutation.isPending || !title.trim() || !body}
        >
          {mutation.isPending ? "Создание..." : "Создать"}
        </Button>
      </div>
    </form>
  );
}
