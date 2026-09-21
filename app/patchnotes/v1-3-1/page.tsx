import { ArrowLeft } from "lucide-react";
import { SectionDivider } from "@/components/patchnotes/section-divider";
import type { Metadata } from "next";
import CustomLink from "@/components/no-prefetch-link";

export const metadata: Metadata = {
  title: "v1.3.1 — Перепись населения",
  description: "Полнотекстный поиск",
};

export default function V130Page() {
  return (
    <main className="min-h-screen bg-background">
      <div className="md:max-w-2/3 max-w-full mx-auto px-6 py-10 md:py-16">
        {/* Back link */}
        <CustomLink
          href="/patchnotes"
          className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors mb-10 group"
        >
          <ArrowLeft
            className="size-3.5 transition-transform duration-150 group-hover:-translate-x-0.5"
            aria-hidden="true"
          />
          К списку
        </CustomLink>

        {/* ── Header ───────────────────────────────────────────────────────── */}
        <header className="mb-10">
          <div className="flex items-center gap-3 mb-4">
            <span className="font-mono text-xs font-semibold px-2.5 py-1 rounded-full bg-muted border border-input text-muted-foreground">
              v1.3.1
            </span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground text-balance mb-2">
            Даже сказать нечего
          </h1>
          <time dateTime="2026-08-05" className="text-sm text-muted-foreground">
            5 августа, 2026
          </time>
        </header>

        {/* ── Main changes ─────────────────────────────────────────────────── */}
        <SectionDivider label="Просто переписал фронт и бек" className="mb-8" />

        <section className="mb-10 space-y-4">
          <p className="text-sm text-foreground leading-relaxed">
            Теперь меньше запросов идет, починил полнотекстный поиск, а и еще оптимизировал рефреш токенов,
            правда из-за этого у кого-то может вход отлететь и нужно будет
            перезайти... Но это мелочи :)
          </p>
          <p className="text 2xl">
            Надеюсь не сдохну пока остатки патча буду писать эх....
          </p>
        </section>

        {/* ── Footer ───────────────────────────────────────────────────────── */}

        <p className="text-2xl italic mb-6">
          Всем бб (бекап на всякий сделал, тесты на проде - лучшие)
        </p>
        <SectionDivider className="mb-8" />
        <CustomLink
          href="/patchnotes"
          className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors group"
        >
          <ArrowLeft
            className="size-3.5 transition-transform duration-150 group-hover:-translate-x-0.5"
            aria-hidden="true"
          />
          Вернуться к списку
        </CustomLink>
      </div>
    </main>
  );
}
