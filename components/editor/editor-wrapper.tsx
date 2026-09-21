"use client";

import { memo } from "react";
import { SlateEditor } from "./slate-editor";
import type { EditorContent } from "@/types/editor";
import { mainPreset } from "@/lib/toolbar/presets";

interface EditorWrapperProps {
  editorContent?: EditorContent;
}

export const EditorWrapper = memo(function EditorWrapper({
  editorContent,
}: EditorWrapperProps) {
  return <SlateEditor initialContent={editorContent} variant={mainPreset} />;
});
