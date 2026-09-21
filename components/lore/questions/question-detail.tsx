"use client";

import { Suspense, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Edit2, Trash2, Check, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { MarkdownRenderer } from "./question-markdown-renderer";
import { AnswerItem } from "./answer-item";
import { SlateEditorSimple } from "./slate-editor-simple";
import type { Question } from "@/types/questions";
import { Skeleton } from "@/components/ui/skeleton";
import { parseMarkdownToSlate, serializeToMarkdown } from "@/lib/editor-utils";
import { Descendant } from "slate";
import { emptyContent } from "@/types/editor";
import {
  createAnswer,
  deleteQuestion,
  getAnswers,
  updateQuestion,
} from "@/server/questions/question";
import { useUser } from "@/lib/contexts/user-context";
import { useParams } from "next/navigation";
import { useRoomId } from "@/lib/room-utils";

interface QuestionDetailProps {
  question: Question;
  onBack: () => void;
  onQuoteClick?: (quoteText: string) => void;
}

function AnswersList({
  questionId,
  onQuoteClick,
}: Readonly<{
  questionId: string;
  onQuoteClick?: (quoteText: string) => void;
}>) {
  const { data: answers, isLoading } = useQuery({
    queryKey: ["answers", questionId],
    queryFn: () => getAnswers(questionId),
  });

  if (isLoading) {
    return (
      <div className="space-y-3">
        {[1, 2].map((i) => (
          <div key={i} className="space-y-2">
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-12 w-full" />
          </div>
        ))}
      </div>
    );
  }

  if (!answers || answers.length === 0) {
    return (
      <p className="text-sm text-muted-foreground italic py-4">
        Пока нет ответов. Будьте первым!
      </p>
    );
  }

  return (
    <div className="divide-y divide-border/50">
      {answers.map((answer) => (
        <AnswerItem
          key={answer.id}
          answer={answer}
          questionId={questionId}
          onQuoteClick={onQuoteClick}
        />
      ))}
    </div>
  );
}

export function QuestionDetail({
  question,
  onBack,
  onQuoteClick,
}: Readonly<QuestionDetailProps>) {
  const queryClient = useQueryClient();
  const currentUser = useUser();
  const isOwner = question.authorId === currentUser.user.id;
  const roomName = useRoomId();

  const [isEditing, setIsEditing] = useState(false);
  const [editTitle, setEditTitle] = useState(question.title);
  const [editBody, setEditBody] = useState(parseMarkdownToSlate(question.body));
  const [newAnswerKey, setNewAnswerKey] = useState(0);
  const [newAnswer, setNewAnswer] = useState<Descendant[]>(emptyContent);

  const updateMutation = useMutation({
    mutationFn: updateQuestion,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["questions"] });
      setIsEditing(false);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteQuestion,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["questions"] });
      onBack();
    },
  });

  const createAnswerMutation = useMutation({
    mutationFn: createAnswer,
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["answers", question.id],
      });
      queryClient.invalidateQueries({ queryKey: ["questions"] });
    },
  });

  const handleUpdate = () => {
    if (!editTitle.trim() || !editBody) return;
    updateMutation.mutate({
      id: question.id,
      title: editTitle,
      body: serializeToMarkdown(editBody),
    });
  };

  const handleDelete = () => {
    deleteMutation.mutate(question.id);
  };

  const handleSubmitAnswer = () => {
    if (!newAnswer || newAnswer.length === 0) return;
    createAnswerMutation.mutate({
      request: {
        questionId: question.id,
        content: serializeToMarkdown(newAnswer),
        roomName: roomName,
      },
    });
    setNewAnswerKey((prev) => prev + 1);
    setNewAnswer(emptyContent);
  };

  const formattedDate = new Date(question.createdAt).toLocaleDateString(
    "ru-RU",
    { day: "numeric", month: "long", year: "numeric" },
  );

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center gap-2 pb-3 border-b border-border mt-2">
        <Button
          variant="ghost"
          size="sm"
          onClick={onBack}
          className="h-8 w-8 p-0"
        >
          <ArrowLeft className="w-4 h-4" />
        </Button>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <span
              className={
                question.isAnswered
                  ? "inline-block w-2 h-2 rounded-full bg-emerald-500"
                  : "inline-block w-2 h-2 rounded-full bg-amber-500"
              }
            />
            <span className="text-xs text-muted-foreground">
              {question.isAnswered ? "Отвечен" : "Без ответа"}
            </span>
          </div>
        </div>
        {isOwner && !isEditing && (
          <div className="flex items-center gap-1">
            <Button
              variant="ghost"
              size="sm"
              className="h-8 w-8 p-0 text-muted-foreground hover:text-foreground"
              onClick={() => setIsEditing(true)}
            >
              <Edit2 className="w-3.5 h-3.5" />
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className="h-8 w-8 p-0 text-muted-foreground hover:text-destructive"
              onClick={handleDelete}
              disabled={deleteMutation.isPending}
            >
              <Trash2 className="w-3.5 h-3.5" />
            </Button>
          </div>
        )}
      </div>

      {/* Scrollable content */}
      <div className="flex-1 overflow-y-auto py-4 px-3 space-y-4">
        {/* Question body */}
        {isEditing ? (
          <div className="space-y-3">
            <input
              type="text"
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
              className="w-full px-3 py-2 text-lg font-semibold bg-background border border-input rounded-md focus:outline-none focus:ring-2 focus:ring-ring text-foreground"
            />
            <SlateEditorSimple
              initialContent={editBody}
              onChange={setEditBody}
            />
            <div className="flex items-center gap-2">
              <Button
                size="sm"
                onClick={handleUpdate}
                disabled={updateMutation.isPending}
                className="gap-1"
              >
                <Check className="w-3.5 h-3.5" />
                Сохранить
              </Button>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => {
                  setIsEditing(false);
                  setEditTitle(question.title);
                  setEditBody(parseMarkdownToSlate(question.body));
                }}
                className="gap-1"
              >
                <X className="w-3.5 h-3.5" />
                Отмена
              </Button>
            </div>
          </div>
        ) : (
          <div>
            <h2 className="text-lg font-semibold text-foreground mb-2">
              {question.title}
            </h2>
            <div className="flex items-center gap-2 mb-3 text-xs text-muted-foreground">
              <span className="font-medium text-foreground/80">
                {question.author}
              </span>
              <span>{formattedDate}</span>
              {question.type.name !== "Общие" && (
                <span className="px-1.5 py-0.5 rounded bg-primary/10 text-primary text-[10px] font-medium">
                  {question.type.name}
                </span>
              )}
              {question.category && (
                <span className="px-1.5 py-0.5 rounded bg-secondary text-secondary-foreground text-[10px] font-medium">
                  {question.category.name}
                </span>
              )}
            </div>
            <MarkdownRenderer
              content={question.body}
              className="text-sm prose prose-sm max-w-none text-foreground/90 prose-p:my-1"
              onQuoteClick={onQuoteClick}
            />
          </div>
        )}

        {/* Answers section */}
        <div className="border-t border-accent pt-4">
          <h3 className="text-sm font-semibold text-foreground mb-3">Ответы</h3>
          <Suspense
            fallback={
              <div className="space-y-3">
                <Skeleton className="h-16 w-full" />
                <Skeleton className="h-16 w-full" />
              </div>
            }
          >
            <AnswersList questionId={question.id} onQuoteClick={onQuoteClick} />
          </Suspense>
        </div>

        {/* New answer form */}
        <div className="border-t border-accent pt-4">
          <h3 className="text-sm font-semibold text-foreground mb-2">
            Ваш ответ
          </h3>
          <SlateEditorSimple
            key={newAnswerKey}
            initialContent={newAnswer}
            onChange={setNewAnswer}
          />
          <div className="flex justify-start mt-2">
            <Button
              size="sm"
              onClick={handleSubmitAnswer}
              disabled={
                createAnswerMutation.isPending || newAnswer.length === 0
              }
            >
              Ответить
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
