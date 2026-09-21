"use client";

import { useState, useEffect, useRef } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ChevronDown,
  ChevronRight,
  Edit2,
  Trash2,
  Reply,
  Shield,
  X,
  Check,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { MarkdownRenderer } from "./question-markdown-renderer";
import { SlateEditorSimple } from "./slate-editor-simple";
import type { Answer } from "@/types/questions";
import { cn } from "@/lib/utils";
import { parseMarkdownToSlate, serializeToMarkdown } from "@/lib/editor-utils";
import { Descendant } from "slate";
import { emptyContent } from "@/types/editor";
import { useUser } from "@/lib/contexts/user-context";
import {
  createAnswer,
  deleteAnswer,
  getDaughterAnswers,
  updateAnswer,
} from "@/server/questions/question";
import { useParams } from "next/navigation";
import { useRoomId } from "@/lib/room-utils";

interface AnswerItemProps {
  answer: Answer;
  questionId: string;
  depth?: number;
  /** Set of answer IDs that are on the path from a root answer down to targetAnswerId */
  expandPath?: Set<string>;
  /** The target answer ID to highlight (from URL ?answerId=) */
  targetAnswerId?: string | null;
  onQuoteClick?: (quoteText: string) => void;
}

const formatDate = (dateValue: Date | string | null) => {
  if (!dateValue) return null;
  const now = new Date();
  const currentYear = now.getFullYear();

  const startOfWeek = new Date(now);
  startOfWeek.setDate(
    now.getDate() - now.getDay() + (now.getDay() === 0 ? -6 : 1),
  );
  startOfWeek.setHours(0, 0, 0, 0);

  const endOfWeek = new Date(startOfWeek);
  endOfWeek.setDate(startOfWeek.getDate() + 6);
  endOfWeek.setHours(23, 59, 59, 999);

  const dateObj = dateValue instanceof Date ? dateValue : new Date(dateValue);
  const year = dateObj.getFullYear();
  const isCurrentYear = year === currentYear;
  const isThisWeek = dateObj >= startOfWeek && dateObj <= endOfWeek;

  const options: Intl.DateTimeFormatOptions = {
    day: "numeric",
    month: "short",
    ...(isCurrentYear ? {} : { year: "numeric" }),
    ...(isThisWeek ? { hour: "2-digit", minute: "2-digit" } : {}),
  };

  return dateObj.toLocaleDateString("ru-RU", options);
};

export function AnswerItem({
  answer,
  questionId,
  depth = 0,
  expandPath,
  targetAnswerId,
  onQuoteClick,
}: Readonly<AnswerItemProps>) {
  const queryClient = useQueryClient();
  const currentUser = useUser();
  const isOwner = answer.authorId === currentUser.user.id;
  const roomName = useRoomId();
  const itemRef = useRef<HTMLDivElement>(null);

  const isOnExpandPath = expandPath?.has(answer.id) ?? false;
  const isTarget = String(answer.id) === String(targetAnswerId);

  // Auto-expand if this answer is on the path to the target
  const [showReplies, setShowReplies] = useState(isOnExpandPath);
  const [isReplying, setIsReplying] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [replyContent, setReplyContent] = useState<Descendant[]>(emptyContent);
  const [editContent, setEditContent] = useState(
    parseMarkdownToSlate(answer.content),
  );
  const [highlightPhase, setHighlightPhase] = useState(0);

  // Keep expansion in sync if expandPath changes (e.g. after data loads)
  useEffect(() => {
    if (isOnExpandPath) {
      setShowReplies(true);
    }
  }, [isOnExpandPath]);

  // Scroll into view and highlight target answer
  useEffect(() => {
    if (!isTarget || !itemRef.current) return;

    requestAnimationFrame(() => {
      itemRef.current?.scrollIntoView({ behavior: "smooth", block: "center" });
    });
  }, [isTarget]);

  const hasPrefetchedReplies = answer.replies && answer.replies.length > 0;

  const { data: fetchedReplies, isLoading: repliesLoading } = useQuery({
    queryKey: ["answers", answer.id],
    queryFn: () => getDaughterAnswers(answer.id),
    enabled:
      showReplies && !hasPrefetchedReplies && (answer.repliesCount ?? 0) > 0,
  });

  const replies = hasPrefetchedReplies ? answer.replies : fetchedReplies;

  const createReplyMutation = useMutation({
    mutationFn: createAnswer,
    onSuccess: (newAnswer) => {
      if (!newAnswer) return;
      queryClient.setQueryData<Answer[]>(["answers", answer.id], (old) => [
        ...(old ?? []),
        newAnswer,
      ]);
      queryClient.setQueryData<Answer[]>(["answers", questionId], (old) =>
        (old ?? []).map((a) =>
          a.id === answer.id
            ? { ...a, repliesCount: (a.repliesCount ?? 0) + 1 }
            : a,
        ),
      );
      setReplyContent(emptyContent);
      setIsReplying(false);
      setShowReplies(true);
    },
  });

  const updateMutation = useMutation({
    mutationFn: updateAnswer,
    onSuccess: (updatedAnswer) => {
      if (!updatedAnswer) return;
      const parentId = answer.parentId;
      queryClient.setQueryData<Answer[]>(["answers", questionId], (old) =>
        old?.map((a) =>
          a.id === updatedAnswer.id
            ? { ...updatedAnswer, parentId: parentId ?? updatedAnswer.parentId }
            : a,
        ),
      );
      if (parentId) {
        queryClient.setQueryData<Answer[]>(["answers", parentId], (old) =>
          old?.map((a) =>
            a.id === updatedAnswer.id
              ? {
                  ...updatedAnswer,
                  parentId: parentId ?? updatedAnswer.parentId,
                }
              : a,
          ),
        );
      }
      setIsEditing(false);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteAnswer,
    onSuccess: () => {
      if (answer.parentId) {
        queryClient.setQueryData<Answer[]>(
          ["answers", answer.parentId],
          (old) => (old ?? []).filter((a) => a.id !== answer.id),
        );
        queryClient.setQueryData<Answer[]>(["answers", questionId], (old) =>
          (old ?? []).map((a) =>
            a.id === answer.parentId
              ? { ...a, repliesCount: Math.max(0, (a.repliesCount ?? 0) - 1) }
              : a,
          ),
        );
      } else {
        queryClient.setQueryData<Answer[]>(["answers", questionId], (old) =>
          (old ?? []).filter((a) => a.id !== answer.id),
        );
      }
    },
  });

  const handleReply = () => {
    if (!replyContent || replyContent.length === 0) return;
    createReplyMutation.mutate({
      request: {
        parentId: answer.id,
        content: serializeToMarkdown(replyContent),
        roomName: roomName,
      },
    });
  };

  const handleUpdate = () => {
    if (!editContent || editContent.length === 0) return;
    updateMutation.mutate({
      id: answer.id,
      content: serializeToMarkdown(editContent),
    });
  };

  const handleDelete = () => {
    deleteMutation.mutate(answer.id);
  };

  const isHighlightOn = highlightPhase % 2 === 1;
  const isFinished = highlightPhase >= 6;

  useEffect(() => {
    if (!isTarget) return;

    let count = 0;

    const interval = setInterval(() => {
      count++;
      setHighlightPhase((p) => p + 1);

      if (count >= 4) {
        clearInterval(interval);
      }
    }, 400); // 0.25с → полный цикл 0.5с (вкл/выкл)

    return () => clearInterval(interval);
  }, [isTarget]);

  const formattedDate = formatDate(answer.createdAt);
  const formattedUpdated = answer.updatedAt
    ? formatDate(answer.updatedAt)
    : null;

  return (
    <div
      ref={itemRef}
      className={cn(
        "group/answer",
        depth > 0 && "ml-4 pl-4 border-l-2 border-input",
        isTarget && "rounded-md px-2",
      )}
      style={
        isTarget
          ? {
              backgroundColor: isHighlightOn
                ? "rgba(59, 130, 246, 0.15)" // мягкий синий
                : "transparent",
              transition: "background-color 0.25s ease",
              boxShadow: isHighlightOn
                ? "0 0 0 1px rgba(59, 130, 246, 0.4)"
                : "",
            }
          : undefined
      }
    >
      <div className="py-3">
        {/* Answer header */}
        <div className="flex items-center gap-2 mb-2">
          <div className="flex items-center gap-1.5">
            <span className="text-sm font-medium text-foreground">
              {typeof answer.author === "string"
                ? answer.author
                : answer.author.name}
            </span>
            {answer.isDM && (
              <span className="inline-flex items-center gap-1 text-[10px] font-semibold px-1.5 py-0.5 rounded-full bg-amber-500/15 text-amber-600 border border-amber-500/30">
                <Shield className="w-2.5 h-2.5" />
                DM
              </span>
            )}
          </div>
          <span className="text-xs text-muted-foreground">{formattedDate}</span>
          {answer.updatedAt && (
            <span className="text-xs text-muted-foreground italic">
              {`( изменено ${formattedUpdated ?? ""})`}
            </span>
          )}
        </div>

        {/* Answer content */}
        {isEditing ? (
          <div className="space-y-2">
            <SlateEditorSimple
              initialContent={editContent}
              onChange={setEditContent}
            />
            <div className="flex items-center gap-2">
              <Button
                size="sm"
                onClick={handleUpdate}
                disabled={updateMutation.isPending}
                className="h-7 gap-1 text-xs"
              >
                <Check className="w-3 h-3" />
                Сохранить
              </Button>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => {
                  setIsEditing(false);
                  setEditContent(emptyContent);
                }}
                className="h-7 gap-1 text-xs"
              >
                <X className="w-3 h-3" />
                Отмена
              </Button>
            </div>
          </div>
        ) : (
          <MarkdownRenderer
            content={answer.content}
            className="text-sm text-foreground/90 prose prose-sm max-w-none prose-p:my-1 wrap-break-word"
            onQuoteClick={onQuoteClick}
          />
        )}

        {/* Answer actions */}
        <div className="flex items-center gap-1 mt-2">
          {(answer.repliesCount ?? 0) > 0 && (
            <Button
              variant="ghost"
              size="sm"
              className="h-7 gap-1 text-xs text-muted-foreground hover:text-foreground"
              onClick={() => setShowReplies(!showReplies)}
            >
              {showReplies ? (
                <ChevronDown className="w-3 h-3" />
              ) : (
                <ChevronRight className="w-3 h-3" />
              )}
              {answer.repliesCount}{" "}
              {answer.repliesCount === 1 ? "ответ" : "ответов"}
            </Button>
          )}

          {depth < 3 && (
            <Button
              variant="ghost"
              size="sm"
              className="h-7 gap-1 text-xs text-muted-foreground hover:text-foreground"
              onClick={() => setIsReplying(!isReplying)}
            >
              <Reply className="w-3 h-3" />
              Ответить
            </Button>
          )}

          {isOwner && !isEditing && (
            <>
              <Button
                variant="ghost"
                size="sm"
                className="h-7 gap-1 text-xs text-muted-foreground hover:text-foreground"
                onClick={() => setIsEditing(true)}
              >
                <Edit2 className="w-3 h-3" />
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="h-7 gap-1 text-xs text-muted-foreground hover:text-destructive"
                onClick={handleDelete}
                disabled={deleteMutation.isPending}
              >
                <Trash2 className="w-3 h-3" />
              </Button>
            </>
          )}
        </div>

        {/* Reply form */}
        {isReplying && (
          <div className="mt-3 space-y-2">
            <SlateEditorSimple
              initialContent={replyContent}
              onChange={setReplyContent}
            />
            <div className="flex items-center gap-2">
              <Button
                size="sm"
                onClick={handleReply}
                disabled={
                  createReplyMutation.isPending || replyContent.length === 0
                }
                className="h-7 gap-1 text-xs"
              >
                Ответить
              </Button>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => {
                  setIsReplying(false);
                  setReplyContent(emptyContent);
                }}
                className="h-7 gap-1 text-xs"
              >
                Отмена
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Nested replies */}
      {showReplies && (
        <div>
          {repliesLoading ? (
            <div className="ml-4 pl-4 py-2">
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <div className="h-3 w-3 animate-spin rounded-full border-2 border-primary border-t-transparent" />
                Загрузка...
              </div>
            </div>
          ) : (
            replies?.map((reply) => (
              <AnswerItem
                key={reply.id}
                answer={reply}
                questionId={questionId}
                depth={depth + 1}
                expandPath={expandPath}
                targetAnswerId={targetAnswerId}
                onQuoteClick={onQuoteClick}
              />
            ))
          )}
        </div>
      )}
    </div>
  );
}
