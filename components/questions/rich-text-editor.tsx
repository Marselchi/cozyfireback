"use client";

import { useState } from "react";
import { Send } from "lucide-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import { Question, CreateAnswerRequest } from "@/types/questions";
import { createAnswer } from "@/server/questions/question";
import { useParams } from "next/navigation";
import { SlateEditorSimple } from "@/components/lore/questions/slate-editor-simple";
import { Descendant } from "slate";
import { serializeToMarkdown } from "@/lib/editor-utils";
import { emptyContent } from "@/types/editor";
import { useRoomId } from "@/lib/room-utils";

interface RichTextEditorProps {
  question: Question | null;
  onReplySuccess: () => void;
}

export function RichTextEditor({
  question,
  onReplySuccess,
}: Readonly<RichTextEditorProps>) {
  const [editorContent, setEditorContent] =
    useState<Descendant[]>(emptyContent);
  const roomName = useRoomId();
  const queryClient = useQueryClient();

  const createAnswerMutation = useMutation({
    mutationFn: createAnswer,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["questions"] });
      toast.success("Ответ успешен!");
      setEditorContent(emptyContent);
      onReplySuccess();
    },
    onError: () => {
      toast.error("Ошибка ответа, попробуйте снова и напишите мне");
    },
  });

  const handleSubmit = () => {
    if (!question || editorContent.length === 0) {
      toast.error("Пожалуйста, не делайте пустой ответ");
      return;
    }

    const request: CreateAnswerRequest = {
      questionId: question.id,
      content: serializeToMarkdown(editorContent).trim(),
      roomName: roomName,
    };

    createAnswerMutation.mutate({ request });
  };

  return (
    <div className="border-t border-input shrink-0">
      {/* Editor */}
      <div className="p-4">
        <SlateEditorSimple
          initialContent={editorContent}
          onChange={setEditorContent}
        />
      </div>
      <div className="flex items-center justify-end p-3 border-b border-input">
        <Button
          onClick={handleSubmit}
          disabled={
            createAnswerMutation.isPending || editorContent.length === 0
          }
          className="bg-blue-500 hover:bg-blue-600 transform hover:scale-105 transition-all duration-200 text-primary"
        >
          <Send className="w-4 h-4 mr-2" />
          {createAnswerMutation.isPending ? "Отвечаю..." : "Ответить"}
        </Button>
      </div>
    </div>
  );
}
