export type ImportExportMode = "import" | "export"

export type ImportFileType = "zip" | "md"

export type ConflictStrategy = "replace" | "keep"

export type SpoilerStrategy = "delete" | "unwrap"

export interface ImportOptions {
  fileType: ImportFileType
  /** ZIP only: replace the entire room content */
  replaceWholeRoom: boolean
  /** ZIP only (when replaceWholeRoom is false): how to handle conflicts */
  conflictStrategy: ConflictStrategy
  /** All file types: attempt to auto-link references */
  tryAutolink: boolean
}

export interface ExportOptions {
  saveTags: boolean
  spoilers: SpoilerStrategy
  dmOnly: boolean
}
