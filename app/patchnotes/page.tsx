import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { patchnotes, formatDate } from "@/lib/patchnotes-registry";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Patch Notes",
  description: "Release notes and changelogs for every version.",
};

export default function PatchnotesPage() {
  return (
    <main className="min-h-screen bg-background">
      <div className="md:max-w-2/3 max-w-full mx-auto px-6 py-16 md:py-24">
        {/* Header */}
        <header className="mb-12">
          <h1 className="text-3xl font-bold tracking-tight text-foreground text-balance mb-3">
            Обновления
          </h1>
          <p className="text-muted-foreground leading-relaxed">
            Поиска нет, новые сверху
          </p>
        </header>

        {/* List */}
        <ul className="space-y-1">
          {patchnotes.map((note) => (
            <li key={note.slug}>
              <Link
                href={`/patchnotes/${note.slug}`}
                className="group flex flex-col gap-2 rounded-xl border border-border bg-card px-5 py-4 transition-colors hover:bg-muted/50 hover:border-foreground/20"
              >
                {/* Top row */}
                <div className="flex items-center justify-between gap-4">
                  <div className="flex items-center gap-2.5 min-w-0">
                    <span className="font-mono text-sm font-semibold text-foreground">
                      v{note.version}
                    </span>
                    {note.label && (
                      <span className="text-[11px] font-medium px-2 py-0.5 rounded-full bg-muted text-muted-foreground border border-input">
                        {note.label}
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <time
                      dateTime={note.date}
                      className="text-xs text-muted-foreground tabular-nums"
                    >
                      {formatDate(note.date)}
                    </time>
                    <ArrowRight
                      className="size-3.5 text-muted-foreground transition-transform duration-150 group-hover:translate-x-0.5"
                      aria-hidden="true"
                    />
                  </div>
                </div>

                {/* Summary */}
                <p className="text-sm text-muted-foreground leading-relaxed">
                  {note.summary}
                </p>
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </main>
  );
}
