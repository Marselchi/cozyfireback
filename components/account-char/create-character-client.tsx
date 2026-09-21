"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ManualCreateForm } from "./manual-create-form";
import { ImportDropzone } from "./import-character-flow/import-dropzone";
import { ImportPreview } from "./import-character-flow/import-preview";
import { useCreateCharacter } from "@/hooks/account-char/use-create-character";
import { useImportCharacter } from "@/hooks/account-char/use-import-character";
import { CharacterCreatePayload } from "@/types/account-char";

interface Props {
  roomId: string;
}

export function CreateCharacterClient({ roomId }: Readonly<Props>) {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<"manual" | "import">("manual");

  // ── Manual creation ──────────────────────────────────────────────────────

  const {
    mutate: createManual,
    isPending: isManualSubmitting,
    error: manualError,
  } = useCreateCharacter(roomId);

  function handleManualSubmit(payload: CharacterCreatePayload) {
    createManual(payload, {
      onSuccess: (character) => {
        router.push(`./self`);
      },
    });
  }

  // ── Import flow ──────────────────────────────────────────────────────────

  const importState = useImportCharacter({roomId});

  // Navigate once the character has been created via import
  useEffect(() => {
    if (importState.step === "done" && importState.savedCharacter) {
      router.push(`./self`);
    }
  }, [importState.step, importState.savedCharacter, roomId, router]);

  const showImportPreview =
    (importState.step === "preview" || importState.step === "submitting") &&
    importState.parseResult?.ok;

  return (
    <Tabs
      value={activeTab}
      onValueChange={(v) => {
        setActiveTab(v as "manual" | "import");
        importState.reset();
      }}
    >
      <TabsList className="mb-6">
        <TabsTrigger value="manual">Вручную</TabsTrigger>
        <TabsTrigger value="import">Импорт (D&amp;D 5e)</TabsTrigger>
      </TabsList>

      {/* Manual tab */}
      <TabsContent value="manual">
        <ManualCreateForm
          roomId={roomId}
          onSubmit={handleManualSubmit}
          isSubmitting={isManualSubmitting}
          submitError={manualError?.message ?? null}
        />
      </TabsContent>

      {/* Import tab */}
      <TabsContent value="import">
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
          />
        )}
      </TabsContent>
    </Tabs>
  );
}
