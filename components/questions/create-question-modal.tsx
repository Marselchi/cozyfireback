"use client";

import type React from "react";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { CategoryBadge } from "./ui/category-badge";
import {
  CATEGORIES,
  type CreateQuestionRequest,
  type QuestionCategory,
} from "@/types/questions";
import { toast } from "sonner";
import { createQuestion } from "@/server/questions/question";
import { useParams } from "next/navigation";
import { SlateEditorSimple } from "@/components/lore/questions/slate-editor-simple";
import { Descendant } from "slate";
import { serializeToMarkdown } from "@/lib/editor-utils";
import { emptyContent } from "@/types/editor";
import { useRoomId } from "@/lib/room-utils";

interface CreateQuestionModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CreateQuestionModal({
  open,
  onOpenChange,
}: Readonly<CreateQuestionModalProps>) {
  const [title, setTitle] = useState("");
  const [body, setBody] = useState<Descendant[]>(emptyContent);
  const [category, setCategory] = useState<QuestionCategory>({
    id: "4",
    name: "Общее",
  });
  const roomName = useRoomId();
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: createQuestion,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["questions"] });
      toast.success("Вопрос успешно создан!");
      onOpenChange(false);
      setTitle("");
      setBody(emptyContent);
      setCategory({ id: "4", name: "Общее" });
    },
    onError: () => {
      toast.error("Возникла ошибка, повторите еще раз и напишите мне");
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || body.length === 0) {
      toast.error("Пожалуйста заполните все поля");
      return;
    }

    const request: CreateQuestionRequest = {
      title: title.trim(),
      body: serializeToMarkdown(body),
      category,
    };

    createMutation.mutate({ request: request, roomName: roomName });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Создание нового вопроса</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="title" className="block text-sm font-medium mb-1">
              Заголовок *
            </label>
            <Input
              id="title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Введите заголовок вопроса"
              className="w-full"
            />
          </div>

          <div>
            <label
              htmlFor="category"
              className="block text-sm font-medium mb-1"
            >
              Категория *
            </label>
            <Select
              value={category.id}
              onValueChange={(id) => {
                const selectedCategory = CATEGORIES.find(
                  (cat) => cat.id === id,
                );
                if (selectedCategory) setCategory(selectedCategory);
              }}
            >
              <SelectTrigger className="w-full">
                <SelectValue>
                  <CategoryBadge category={category} size="sm" />
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                {CATEGORIES.map((cat) => (
                  <SelectItem key={cat.id} value={cat.id}>
                    <CategoryBadge category={cat} size="sm" />
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div>
            <label htmlFor="body" className="block text-sm font-medium mb-1">
              Описание *
            </label>
            <SlateEditorSimple initialContent={body} onChange={setBody} />
          </div>

          <div className="flex justify-end space-x-2 pt-4">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={createMutation.isPending}
            >
              Отмена
            </Button>
            <Button
              type="submit"
              disabled={createMutation.isPending}
              className="bg-primary hover:bg-primary/90 transform transition-all duration-200"
            >
              {createMutation.isPending ? "Создаю..." : "Создать вопрос"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
