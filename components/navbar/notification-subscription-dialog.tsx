"use client";

import { useEffect, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import {
  FileText,
  HelpCircle,
  RefreshCw,
  Bell,
  ChevronDown,
  ChevronRight,
  Check,
} from "lucide-react";
import {
  NotificationSetting,
  SettingKey,
  getNotificationSettings,
  saveNotificationSettings,
} from "@/server/notifications/notifications";
import { useParams } from "next/navigation";
import { useRoomId } from "@/lib/room-utils";

/* ─── Query key ──────────────────────────────────────────────────────────── */
const SETTINGS_KEY = ["notifications", "settings"] as const;

/* ─── Types ──────────────────────────────────────────────────────────────── */
type DeliveryKey = "polling" | "push";

type ContentKey = "contentAll" | "contentDMOnly" | "contentUpdated";
type QAKey = "qaNewQuestion" | "qaAnswered" | "qaAnswerToAnswer";

const CONTENT_KEYS: ContentKey[] = [
  "contentAll",
  "contentDMOnly",
  "contentUpdated",
];
const QA_KEYS: QAKey[] = ["qaNewQuestion", "qaAnswered", "qaAnswerToAnswer"];

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/* ─── Helpers ────────────────────────────────────────────────────────────── */
function findSetting(
  list: NotificationSetting[],
  key: SettingKey,
): NotificationSetting {
  return (
    list.find((s) => s.key === key) ?? { key, polling: false, push: false }
  );
}

function isCategoryAllSelected(
  list: NotificationSetting[],
  keys: SettingKey[],
  delivery: DeliveryKey,
): boolean {
  return keys.every((k) => findSetting(list, k)[delivery]);
}

function isCategoryIndeterminate(
  list: NotificationSetting[],
  keys: SettingKey[],
  delivery: DeliveryKey,
): boolean {
  const checked = keys.filter((k) => findSetting(list, k)[delivery]);
  return checked.length > 0 && checked.length < keys.length;
}

function updateSetting(
  list: NotificationSetting[],
  key: SettingKey,
  delivery: DeliveryKey,
  value: boolean,
): NotificationSetting[] {
  return list.map((s) => (s.key === key ? { ...s, [delivery]: value } : s));
}

/* ─── Sub-components ─────────────────────────────────────────────────────── */
function DeliveryHeader() {
  return (
    <div className="flex items-center justify-end gap-6 px-4 pb-2 border-b border-border">
      <div className="flex items-center gap-1.5 text-xs font-semibold text-muted-foreground uppercase tracking-wider w-14 justify-center">
        <RefreshCw className="w-3.5 h-3.5" />
        <span>Сайт</span>
      </div>
      <div className="flex items-center gap-1.5 text-xs font-semibold text-muted-foreground uppercase tracking-wider w-14 justify-center">
        <Bell className="w-3.5 h-3.5" />
        <span>Пуш</span>
      </div>
    </div>
  );
}

interface NotifRowProps {
  label: string;
  description?: string;
  pollingChecked: boolean;
  pushChecked: boolean;
  pollingDisabled?: boolean;
  pushDisabled?: boolean;
  indent?: boolean;
  onToggle: (delivery: DeliveryKey) => void;
}

function NotifRow({
  label,
  description,
  pollingChecked,
  pushChecked,
  pollingDisabled,
  pushDisabled,
  indent,
  onToggle,
}: Readonly<NotifRowProps>) {
  return (
    <div
      className={cn(
        "flex items-center justify-between px-4 py-2.5 rounded-md transition-colors hover:bg-accent/60 group",
        indent && "pl-8",
      )}
    >
      <div className="flex-1 min-w-0 pr-4">
        <p className="text-sm font-medium text-foreground leading-tight">
          {label}
        </p>
        {description && (
          <p className="text-xs text-muted-foreground mt-0.5 leading-snug">
            {description}
          </p>
        )}
      </div>
      <div className="flex items-center gap-6">
        <div className="w-14 flex justify-center">
          <Checkbox
            checked={pollingChecked}
            disabled={pollingDisabled}
            onCheckedChange={() => onToggle("polling")}
            className={cn(
              "h-4 w-4 rounded",
              pollingDisabled && "opacity-40 cursor-not-allowed",
            )}
            aria-label={`${label} Сайт`}
          />
        </div>
        <div className="w-14 flex justify-center">
          <Checkbox
            checked={pushChecked}
            disabled={pushDisabled}
            onCheckedChange={() => onToggle("push")}
            className={cn(
              "h-4 w-4 rounded",
              pushDisabled && "opacity-40 cursor-not-allowed",
            )}
            aria-label={`${label} Пуш`}
          />
        </div>
      </div>
    </div>
  );
}

interface CategoryHeaderRowProps {
  label: string;
  icon: React.ReactNode;
  colorClass: string;
  expanded: boolean;
  onToggleExpand: () => void;
  pollingAllChecked: boolean;
  pollingIndeterminate: boolean;
  pushAllChecked: boolean;
  pushIndeterminate: boolean;
  onSelectAll: (delivery: DeliveryKey, value: boolean) => void;
}

function CategoryHeaderRow({
  label,
  icon,
  colorClass,
  expanded,
  onToggleExpand,
  pollingAllChecked,
  pollingIndeterminate,
  pushAllChecked,
  pushIndeterminate,
  onSelectAll,
}: Readonly<CategoryHeaderRowProps>) {
  return (
    <div className="flex items-center justify-between px-4 py-2.5 rounded-lg bg-muted/60">
      <button
        onClick={onToggleExpand}
        className="flex items-center gap-2.5 flex-1 text-left group"
        aria-expanded={expanded}
      >
        <span
          className={cn(
            "flex items-center justify-center w-6 h-6 rounded-md",
            colorClass,
          )}
        >
          {icon}
        </span>
        <span className="text-sm font-semibold text-foreground">{label}</span>
        <span className="ml-1 text-muted-foreground transition-transform duration-200">
          {expanded ? (
            <ChevronDown className="w-3.5 h-3.5" />
          ) : (
            <ChevronRight className="w-3.5 h-3.5" />
          )}
        </span>
      </button>
      <div className="flex items-center gap-6">
        <div className="w-14 flex flex-col items-center gap-0.5">
          <Checkbox
            checked={pollingIndeterminate ? "indeterminate" : pollingAllChecked}
            onCheckedChange={(v) => onSelectAll("polling", !!v)}
            className="h-4 w-4 rounded"
            aria-label={`Выбрать все ${label} сайта`}
          />
          <span className="text-[9px] text-muted-foreground font-medium">
            Все
          </span>
        </div>
        <div className="w-14 flex flex-col items-center gap-0.5">
          <Checkbox
            checked={pushIndeterminate ? "indeterminate" : pushAllChecked}
            onCheckedChange={(v) => onSelectAll("push", !!v)}
            className="h-4 w-4 rounded"
            aria-label={`Выбрать все ${label} пуш`}
          />
          <span className="text-[9px] text-muted-foreground font-medium">
            Все
          </span>
        </div>
      </div>
    </div>
  );
}

/* ─── Main component ─────────────────────────────────────────────────────── */
export function NotificationSubscriptionDialog({
  open,
  onOpenChange,
}: Readonly<Props>) {
  const queryClient = useQueryClient();
  const roomName = useRoomId();

  const { data: serverSettings } = useQuery({
    queryKey: SETTINGS_KEY,
    queryFn: () => getNotificationSettings(roomName),
    enabled: open,
  });

  // Local draft — initialised from server data when dialog opens
  const [draft, setDraft] = useState<NotificationSetting[]>([]);
  const [contentExpanded, setContentExpanded] = useState(true);
  const [qaExpanded, setQaExpanded] = useState(true);

  useEffect(() => {
    if (serverSettings) {
      // Гарантируем, что у нас есть все ключи, даже если бэкенд что-то пропустил
      const allKeys: SettingKey[] = [...CONTENT_KEYS, ...QA_KEYS];

      const fullDraft = allKeys.map((key) => {
        const existing = serverSettings.find((s) => s.key === key);
        return existing ?? { key, polling: false, push: false };
      });

      setDraft(fullDraft);
    } else {
      // Если данных нет (например, первая загрузка), инициализируем пустыми значениями
      // чтобы интерфейс не падал
      const allKeys: SettingKey[] = [...CONTENT_KEYS, ...QA_KEYS];
      setDraft(allKeys.map((key) => ({ key, polling: false, push: false })));
    }
  }, [serverSettings]);

  const save = useMutation({
    mutationFn: (settings: NotificationSetting[]) =>
      saveNotificationSettings(roomName, settings),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SETTINGS_KEY });
      onOpenChange(false);
    },
  });

  /* ─── Helpers ── */
  function toggle(key: SettingKey, delivery: DeliveryKey) {
    setDraft((prev) => {
      const current = findSetting(prev, key)[delivery];
      let next = updateSetting(prev, key, delivery, !current);

      // contentAll + contentDMOnly are mutually exclusive per delivery
      if (key === "contentAll" && !current) {
        next = updateSetting(next, "contentDMOnly", delivery, false);
      }
      if (key === "contentDMOnly" && !current) {
        next = updateSetting(next, "contentAll", delivery, false);
      }

      return next;
    });
  }

  function selectAllCategory(
    keys: SettingKey[],
    delivery: DeliveryKey,
    value: boolean,
  ) {
    setDraft((prev) => {
      let next = prev;
      keys.forEach((k) => {
        next = updateSetting(next, k, delivery, value);
      });

      // When selecting all content, remove contentDMOnly (it's redundant)
      if (
        value &&
        keys.includes("contentAll") &&
        keys.includes("contentDMOnly")
      ) {
        next = updateSetting(next, "contentDMOnly", delivery, false);
      }

      return next;
    });
  }

  function handleReset() {
    setDraft(
      serverSettings?.map((s) => ({ ...s, polling: false, push: false })) ?? [],
    );
  }

  const totalActive = draft.reduce(
    (acc, s) => acc + (s.polling ? 1 : 0) + (s.push ? 1 : 0),
    0,
  );

  const contentAllPoll = findSetting(draft, "contentAll").polling;
  const contentAllPush = findSetting(draft, "contentAll").push;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg w-full p-0 gap-0 overflow-hidden rounded-xl border border-border shadow-xl">
        {/* Header */}
        <DialogHeader className="px-6 pt-6 pb-4 border-b border-border bg-card">
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <DialogTitle className="text-base font-semibold text-foreground">
                Настройки уведомлений
              </DialogTitle>
              <DialogDescription className="text-sm text-muted-foreground mt-0.5">
                Выберите, какие уведомления и как хотите получать
              </DialogDescription>
            </div>
            {totalActive > 0 && (
              <Badge
                variant="secondary"
                className="ml-3 mt-3 text-xs font-semibold bg-primary/10 text-primary border-primary/20 pointer-events-none"
              >
                {totalActive} активных
              </Badge>
            )}
          </div>

          {/* Delivery legend */}
          <div className="flex gap-4 mt-3">
            <div className="flex items-center gap-2 text-xs text-muted-foreground bg-muted/60 rounded-md px-3 py-1.5">
              <RefreshCw className="w-3.5 h-3.5 text-primary" />
              <span>
                <span className="font-semibold text-foreground">На сайте</span>{" "}
                — каждую минуту
              </span>
            </div>
            <div className="flex items-center gap-2 text-xs text-muted-foreground bg-muted/60 rounded-md px-3 py-1.5">
              <Bell className="w-3.5 h-3.5 text-primary" />
              <span>
                <span className="font-semibold text-foreground">
                  Пуш (пока выключен)
                </span>{" "}
                — На устройства
              </span>
            </div>
          </div>
        </DialogHeader>

        {/* Body */}
        <div className="px-6 py-4 space-y-4 max-h-[60vh] overflow-y-auto bg-background">
          <DeliveryHeader />

          {/* Content category */}
          <div className="space-y-1">
            <CategoryHeaderRow
              label="Контент"
              icon={
                <FileText className="w-3.5 h-3.5 text-blue-600 dark:text-blue-400" />
              }
              colorClass="bg-blue-500/10"
              expanded={contentExpanded}
              onToggleExpand={() => setContentExpanded((v) => !v)}
              pollingAllChecked={isCategoryAllSelected(
                draft,
                CONTENT_KEYS,
                "polling",
              )}
              pollingIndeterminate={isCategoryIndeterminate(
                draft,
                CONTENT_KEYS,
                "polling",
              )}
              pushAllChecked={isCategoryAllSelected(
                draft,
                CONTENT_KEYS,
                "push",
              )}
              pushIndeterminate={isCategoryIndeterminate(
                draft,
                CONTENT_KEYS,
                "push",
              )}
              onSelectAll={(d, v) => selectAllCategory(CONTENT_KEYS, d, v)}
            />

            {contentExpanded && (
              <div className="mt-1 space-y-0.5">
                <NotifRow
                  label="Новый контент (все)"
                  description="Когда новая статья/персонаж добавлен в комнату"
                  pollingChecked={findSetting(draft, "contentAll").polling}
                  pushChecked={findSetting(draft, "contentAll").push}
                  indent
                  onToggle={(d) => toggle("contentAll", d)}
                />
                <NotifRow
                  label="Новый контент (DM)"
                  description="Когда новая статья/персонаж добавлен в комнату DM-ом"
                  pollingChecked={findSetting(draft, "contentDMOnly").polling}
                  pushChecked={findSetting(draft, "contentDMOnly").push}
                  pollingDisabled={contentAllPoll}
                  pushDisabled={contentAllPush}
                  indent
                  onToggle={(d) => toggle("contentDMOnly", d)}
                />
                {(contentAllPoll || contentAllPush) && (
                  <p className="pl-8 text-[11px] text-amber-600 dark:text-amber-400 flex items-center gap-1.5 pb-1 mt-1">
                    <span className="inline-flex items-center justify-center w-3.5 h-3.5 rounded-full bg-amber-100 dark:bg-amber-900/40 shrink-0">
                      <span className="text-[9px] font-bold">!</span>
                    </span>
                    <span>
                      &ldquo;Новый контент (все)&rdquo; уже включает DM опцию
                    </span>
                  </p>
                )}
                <NotifRow
                  label="Обновленый контент"
                  description="Когда прочитаная вами статья/персонаж обновляется"
                  pollingChecked={findSetting(draft, "contentUpdated").polling}
                  pushChecked={findSetting(draft, "contentUpdated").push}
                  indent
                  onToggle={(d) => toggle("contentUpdated", d)}
                />
              </div>
            )}
          </div>

          {/* Q&A category */}
          <div className="space-y-1">
            <CategoryHeaderRow
              label="Вопросы/Ответы"
              icon={
                <HelpCircle className="w-3.5 h-3.5 text-violet-600 dark:text-violet-400" />
              }
              colorClass="bg-violet-500/10"
              expanded={qaExpanded}
              onToggleExpand={() => setQaExpanded((v) => !v)}
              pollingAllChecked={isCategoryAllSelected(
                draft,
                QA_KEYS,
                "polling",
              )}
              pollingIndeterminate={isCategoryIndeterminate(
                draft,
                QA_KEYS,
                "polling",
              )}
              pushAllChecked={isCategoryAllSelected(draft, QA_KEYS, "push")}
              pushIndeterminate={isCategoryIndeterminate(
                draft,
                QA_KEYS,
                "push",
              )}
              onSelectAll={(d, v) => selectAllCategory(QA_KEYS, d, v)}
            />

            {qaExpanded && (
              <div className="mt-1 space-y-0.5">
                <NotifRow
                  label="Новый вопрос"
                  description="Когда появился новый вопрос в комнате"
                  pollingChecked={findSetting(draft, "qaNewQuestion").polling}
                  pushChecked={findSetting(draft, "qaNewQuestion").push}
                  indent
                  onToggle={(d) => toggle("qaNewQuestion", d)}
                />
                <NotifRow
                  label="Ответ на ваш вопрос"
                  description="Когда кто-то ответил на ваш вопрос"
                  pollingChecked={findSetting(draft, "qaAnswered").polling}
                  pushChecked={findSetting(draft, "qaAnswered").push}
                  indent
                  onToggle={(d) => toggle("qaAnswered", d)}
                />
                <NotifRow
                  label="Ответ на ваш ответ"
                  description="Когда кто-то ответил на ваш ответ"
                  pollingChecked={
                    findSetting(draft, "qaAnswerToAnswer").polling
                  }
                  pushChecked={findSetting(draft, "qaAnswerToAnswer").push}
                  indent
                  onToggle={(d) => toggle("qaAnswerToAnswer", d)}
                />
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <DialogFooter className="px-6 py-4 border-t border-border bg-card flex items-center justify-between">
          <Button
            variant="ghost"
            size="sm"
            onClick={handleReset}
            className="text-muted-foreground hover:text-foreground"
          >
            Сбросить всё
          </Button>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => onOpenChange(false)}
            >
              Отмена
            </Button>
            <Button
              size="sm"
              onClick={() => save.mutate(draft)}
              disabled={save.isPending}
              className="gap-1.5"
            >
              <Check className="w-3.5 h-3.5" />
              Сохранить
            </Button>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
