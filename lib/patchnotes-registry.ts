export interface PatchnoteEntry {
  /** Display version string, e.g. "0.2.0" */
  version: string;
  /** URL slug used in the route, e.g. "v0-2-0" → /patchnotes/v0-2-0 */
  slug: string;
  /** ISO date string */
  date: string;
  /** One-line description shown on the list page */
  summary: string;
  /** Optional label shown as a badge next to the version */
  label?: string;
}

/**
 * Central registry of all patchnotes.
 *
 * To add a new patchnote:
 *   1. Add an entry at the TOP of this array (newest first).
 *   2. Create the page at  app/patchnotes/<slug>/page.tsx
 *      — copy an existing page as your starting point.
 */
export const patchnotes: PatchnoteEntry[] = [
  {
    version: "2.0.1",
    slug: "v2-0-1",
    date: "2026-09-21",
    summary: "Больше роллов",
    label: "Последнее",
  },
  {
    version: "2.0.0",
    slug: "v2-0-0",
    date: "2026-09-18",
    summary: "ПОЛНАЯ ПИЗДА",
    label: "MAJOR",
  },
  {
    version: "1.3.1",
    slug: "v1-3-1",
    date: "2026-08-05",
    summary: "Небольшая перепись",
    label: "minor",
  },
  {
    version: "1.3.0",
    slug: "v1-3-0",
    date: "2026-07-19",
    summary: "Просто поиск",
    label: "major",
  },
  {
    version: "1.2.0",
    slug: "v1-2-0",
    date: "2026-07-11",
    summary: "Таблички",
    label: "major",
  },
  {
    version: "1.1.0",
    slug: "v1-1-0",
    date: "2026-07-08",
    summary:
      "Первый крупный патч после защиты диплома. Полная переработка архитектуры.",
    label: "major",
  },
  // {
  //   version: "0.1.0",
  //   slug: "v0-1-0",
  //   date: "2025-06-10",
  //   summary:
  //     "Block reference — every available content block in one place. Use this page as a template when authoring new patchnotes.",
  //   label: "Reference",
  // },
];

/** Formats an ISO date string into a human-readable form, e.g. "Jul 1, 2025" */
export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("ru-RU", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}
