"use client";

import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { ImportDropzone } from "./import-dropzone";
import { ImportPreview } from "./import-preview";
import { useImportCharacter } from "@/hooks/account-char/use-import-character";

interface ImportDialogProps {
  roomId: string;
  characterId?: string;
  isSelf?: boolean;
  trigger?: React.ReactNode;
}

export function ImportDialog({
  roomId,
  characterId,
  isSelf,
  trigger,
}: Readonly<ImportDialogProps>) {
  const [open, setOpen] = useState(false);
  const importState = useImportCharacter({
    roomId,
    characterId,
    isSelf,
    onSuccess: () => setOpen(false),
  });

  const showImportPreview =
    (importState.step === "preview" || importState.step === "submitting") &&
    importState.parseResult?.ok;

  const handleOpenChange = (isOpen: boolean) => {
    if (!isOpen) {
      importState.reset();
    }
    setOpen(isOpen);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        {trigger || (
          <Button variant="outline">
            {importState.isUpdateMode
              ? "Обновить из файла"
              : "Импорт персонажа"}
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="sm:max-w-150 max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {importState.isUpdateMode
              ? "Обновление персонажа"
              : "Импорт персонажа"}
          </DialogTitle>
          <DialogDescription>
            {importState.isUpdateMode
              ? "Загрузите файл, чтобы перезаписать данные текущего персонажа."
              : "Загрузите файл, чтобы создать нового персонажа."}
          </DialogDescription>
        </DialogHeader>

        <div className="py-4">
          {(importState.step === "idle" || importState.step === "error") && (
            <ImportDropzone
              onParsed={importState.parseFile}
              parseError={importState.parseError}
            />
          )}
          {showImportPreview && importState.parseResult?.ok && (
            <ImportPreview
              character={importState.parseResult.character}
              onConfirm={importState.confirm}
              onCancel={importState.reset}
              isSubmitting={importState.step === "submitting"}
              submitError={importState.submitError}
              // Опционально: передайте проп isUpdate={importState.isUpdateMode}
              // в ImportPreview, чтобы менять текст кнопки подтверждения
            />
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
