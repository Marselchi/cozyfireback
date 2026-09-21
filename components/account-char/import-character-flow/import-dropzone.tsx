"use client";

import { useCallback, useRef, useState } from "react";
import { UploadIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

interface Props {
  onParsed: (raw: unknown) => void;
  parseError?: string | null;
}

export function ImportDropzone({ onParsed, parseError }: Readonly<Props>) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);
  const [pasteValue, setPasteValue] = useState("");
  const [fileError, setFileError] = useState<string | null>(null);

  function parseAndEmit(text: string) {
    setFileError(null);
    try {
      const raw = JSON.parse(text);
      onParsed(raw);
    } catch {
      setFileError(
        "Скорее всего вы закинули не тот файл, потому что я не могу его распарсить :с.",
      );
    }
  }

  function handleFile(file: File) {
    if (!file.name.endsWith(".json") && file.type !== "application/json") {
      setFileError("Только .json файлы");
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => parseAndEmit(e.target?.result as string);
    reader.onerror = () => setFileError("Ошибка чтения файла.");
    reader.readAsText(file);
  }

  const handleDrop = useCallback(
    (e: React.DragEvent<HTMLDivElement>) => {
      e.preventDefault();
      setDragging(false);
      const file = e.dataTransfer.files?.[0];
      if (file) handleFile(file);
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDragging(true);
  };

  const handleDragLeave = () => setDragging(false);

  function handleFileInput(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
    // reset input so the same file can be selected again
    e.target.value = "";
  }

  function handlePasteSubmit() {
    if (!pasteValue.trim()) return;
    parseAndEmit(pasteValue.trim());
  }

  const error = fileError || parseError;

  return (
    <div className="space-y-6">
      {/* Drop zone */}
      <div
        role="region"
        aria-label="Drop zone for JSON character file"
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        className={cn(
          "flex flex-col items-center justify-center rounded-lg border-2 border-dashed p-10 transition-colors cursor-pointer",
          dragging
            ? "border-primary bg-primary/5"
            : "border-input hover:border-primary/60",
        )}
        onClick={() => fileInputRef.current?.click()}
        onKeyDown={(e) => e.key === "Enter" && fileInputRef.current?.click()}
        tabIndex={0}
      >
        <UploadIcon
          className="h-8 w-8 text-muted-foreground mb-3"
          aria-hidden="true"
        />
        <p className="text-sm font-medium text-foreground">
          Закиньте JSON файл сюда
        </p>
        <p className="text-xs text-muted-foreground mt-1">
          или кликните чтобы открыть проводник
        </p>
        <input
          ref={fileInputRef}
          type="file"
          accept=".json,application/json"
          className="sr-only"
          onChange={handleFileInput}
          aria-label="Upload JSON character export"
        />
      </div>

      {/* Paste fallback */}
      <div className="space-y-2">
        <Label htmlFor="import-paste">
          Можете вставить содердимое файла напрямую
        </Label>
        <Textarea
          id="import-paste"
          value={pasteValue}
          onChange={(e) => setPasteValue(e.target.value)}
          placeholder={'{\n  "data": "..."\n}'}
          rows={6}
          className="font-mono text-xs border-input"
        />
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={handlePasteSubmit}
          disabled={!pasteValue.trim()}
        >
          Загрузить
        </Button>
      </div>

      {error && (
        <p role="alert" className="text-sm text-destructive">
          {error}
        </p>
      )}
    </div>
  );
}
