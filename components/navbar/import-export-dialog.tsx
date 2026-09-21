"use client";

import { useRef, useState } from "react";
import { Upload } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import type {
  ImportExportMode,
  ImportFileType,
  ConflictStrategy,
  SpoilerStrategy,
  ImportOptions,
  ExportOptions,
} from "@/types/import-export";
import { importRoomAction } from "@/server/room/actions";
import { useParams } from "next/navigation";
import { exportRoomAction } from "@/server/room/export";
import { useRoomId } from "@/lib/room-utils";

interface ImportExportDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function ImportExportDialog({
  open,
  onOpenChange,
}: ImportExportDialogProps) {
  const [mode, setMode] = useState<ImportExportMode>("import");
  const roomName = useRoomId();

  // --- import state ---
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileType, setFileType] = useState<ImportFileType | null>(null);
  const [replaceWholeRoom, setReplaceWholeRoom] = useState(false);
  const [conflictStrategy, setConflictStrategy] =
    useState<ConflictStrategy>("replace");
  const [tryAutolink, setTryAutolink] = useState(false);

  // --- export state ---
  const [saveTags, setSaveTags] = useState(true);
  const [spoilers, setSpoilers] = useState<SpoilerStrategy>("unwrap");
  const [dmOnly, setDmOnly] = useState(false);

  const [loading, setLoading] = useState(false);

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0] ?? null;
    setSelectedFile(file);
    if (file) {
      setFileType(file.name.endsWith(".zip") ? "zip" : "md");
    } else {
      setFileType(null);
    }
  }

  function resetImportState() {
    setSelectedFile(null);
    setFileType(null);
    setReplaceWholeRoom(false);
    setConflictStrategy("replace");
    setTryAutolink(false);
    if (fileInputRef.current) fileInputRef.current.value = "";
  }

  function handleClose() {
    resetImportState();
    onOpenChange(false);
  }

  async function handleConfirm() {
    setLoading(true);
    try {
      if (mode === "import") {
        if (!selectedFile) return;
        const formData = new FormData();
        formData.append("file", selectedFile);
        const options: ImportOptions = {
          fileType: fileType!,
          replaceWholeRoom: fileType === "zip" ? replaceWholeRoom : false,
          conflictStrategy:
            fileType === "zip" && !replaceWholeRoom
              ? conflictStrategy
              : "replace",
          tryAutolink,
        };
        await importRoomAction(formData, options, roomName);
      } else {
        const params = new URLSearchParams({
          saveTags: String(saveTags),
          saveSpoilers: String(spoilers === "unwrap"), // или преобразуй в true/false строки, как требует бэк
          dmOnly: String(dmOnly),
        });

        const url = `/api/export/${roomName}?${params.toString()}`;
        const link = document.createElement("a");
        link.href = url;
        link.click();
        link.remove();
      }
      handleClose();
    } finally {
      setLoading(false);
    }
  }

  const isImportDisabled = mode === "import" && !selectedFile;

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogDescription />
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Импорт / Экспорт</DialogTitle>
        </DialogHeader>

        {/* Mode switcher */}
        <Tabs
          value={mode}
          onValueChange={(v) => {
            setMode(v as ImportExportMode);
            resetImportState();
          }}
          className="w-full"
        >
          <TabsList className="w-full">
            <TabsTrigger value="import" className="flex-1">
              Импорт
            </TabsTrigger>
            <TabsTrigger value="export" className="flex-1">
              Экспорт
            </TabsTrigger>
          </TabsList>
        </Tabs>

        {/* ── IMPORT ── */}
        {mode === "import" && (
          <div className="flex flex-col gap-4 py-2">
            {/* File upload */}
            <div className="flex flex-col gap-2">
              <Label htmlFor="import-file">Файл (.zip или .md)</Label>
              <label
                htmlFor="import-file"
                className="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed border-muted-foreground/30 px-4 py-6 text-sm text-muted-foreground transition-colors hover:border-muted-foreground/60 hover:text-foreground"
              >
                <Upload className="h-5 w-5" />
                {selectedFile ? (
                  <span className="font-medium text-foreground">
                    {selectedFile.name}
                  </span>
                ) : (
                  <span>Нажмите для выбора файла</span>
                )}
                <input
                  ref={fileInputRef}
                  id="import-file"
                  type="file"
                  accept=".zip,.md"
                  className="sr-only"
                  onChange={handleFileChange}
                />
              </label>
            </div>

            {/* ZIP-only options */}
            {fileType === "zip" && (
              <>
                <div className="flex items-center justify-between">
                  <Label htmlFor="replace-room">Заменить всю комнату?</Label>
                  <Switch
                    id="replace-room"
                    checked={replaceWholeRoom}
                    onCheckedChange={setReplaceWholeRoom}
                  />
                </div>

                {!replaceWholeRoom && (
                  <div className="flex flex-col gap-1.5">
                    <Label htmlFor="conflict-strategy">При конфликте</Label>
                    <Select
                      value={conflictStrategy}
                      onValueChange={(v) =>
                        setConflictStrategy(v as ConflictStrategy)
                      }
                    >
                      <SelectTrigger id="conflict-strategy">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="replace">Заменить новой</SelectItem>
                        <SelectItem value="keep">Оставить старую</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                )}
              </>
            )}

            {/* Common option */}
            {fileType && (
              <div className="flex items-center justify-between">
                <Label htmlFor="autolink">
                  Попробовать автопривязать ссылки?
                </Label>
                <Switch
                  id="autolink"
                  checked={tryAutolink}
                  onCheckedChange={setTryAutolink}
                />
              </div>
            )}
          </div>
        )}

        {/* ── EXPORT ── */}
        {mode === "export" && (
          <div className="flex flex-col gap-4 py-2">
            <div className="flex items-center justify-between">
              <Label htmlFor="save-tags">Сохранить теги?</Label>
              <Switch
                id="save-tags"
                checked={saveTags}
                onCheckedChange={setSaveTags}
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="spoilers">Спойлеры</Label>
              <Select
                value={spoilers}
                onValueChange={(v) => setSpoilers(v as SpoilerStrategy)}
              >
                <SelectTrigger id="spoilers">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="delete">Удалить</SelectItem>
                  <SelectItem value="unwrap">Раскрыть</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="flex items-center justify-between">
              <Label htmlFor="dm-only">Статьи только от ДМ?</Label>
              <Switch
                id="dm-only"
                checked={dmOnly}
                onCheckedChange={setDmOnly}
              />
            </div>
          </div>
        )}

        <DialogFooter className="gap-2 sm:gap-0">
          <Button variant="outline" onClick={handleClose} disabled={loading}>
            Отменить
          </Button>
          <Button
            onClick={handleConfirm}
            disabled={loading || isImportDisabled}
          >
            {loading ? "Выполняется..." : "Подтвердить"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
