"use client";

import { useEffect, useState } from "react";
import { Circle, LayoutTemplate } from "lucide-react";
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
import type { Descendant } from "slate";
import { serializeToMarkdown } from "@/lib/editor-utils";
import { createTemplate } from "@/server/templates/templates";
import { useParams } from "next/navigation";
import { useRoomId } from "@/lib/room-utils";

type SaveTemplateDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** The current editor content to be saved as a template */
  content: Descendant[];
};

export function SaveTemplateDialog({
  open,
  onOpenChange,
  content,
}: Readonly<SaveTemplateDialogProps>) {
  const [name, setName] = useState("");
  const [saving, setSaving] = useState(false);
  const roomName = useRoomId();
  // Reset name whenever dialog opens
  useEffect(() => {
    if (open) setName("");
  }, [open]);

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();
    e.stopPropagation();

    const trimmed = name.trim();
    if (!trimmed) return;

    setSaving(true);
    try {
      const markdown = serializeToMarkdown(content);
      const result = await createTemplate(roomName, trimmed, markdown);
      if (result.success) {
        onOpenChange(false);
      }
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!saving) onOpenChange(false);
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(v) => {
        if (!saving) onOpenChange(v);
      }}
      modal
    >
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <LayoutTemplate className="h-5 w-5" />
            Сохранить как шаблон
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="grid gap-2">
            <Label htmlFor="template-name">Название шаблона</Label>
            <Input
              id="template-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Введите название..."
              autoFocus
              disabled={saving}
              required
            />
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={handleCancel}
              disabled={saving}
            >
              Отмена
            </Button>
            <Button type="submit" disabled={!name.trim() || saving}>
              {saving ? (
                <span className="flex items-center gap-2">
                  <Circle className="h-4 w-4" />
                  Сохранение...
                </span>
              ) : (
                "Сохранить"
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
