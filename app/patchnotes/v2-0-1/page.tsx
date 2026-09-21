import { ArrowLeft } from "lucide-react";
import { SectionDivider } from "@/components/patchnotes/section-divider";
import type { Metadata } from "next";
import CustomLink from "@/components/no-prefetch-link";
import { DiceFormulaNode } from "@/lib/editor/renders/dice-formula";
import { Demo } from "./renders";

export const metadata: Metadata = {
  title: "v2.0.1 — Роллы, больше роллов",
  description: "Не украл, а вдохновился",
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
              v2.0.1
            </span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground text-balance mb-2">
            Лучшие идеи можно подсмотреть у кого-нибудь другого.
          </h1>
          <time dateTime="2026-09-21" className="text-sm text-muted-foreground">
            21 сентября, 2026
          </time>
        </header>

        {/* ── Main changes ─────────────────────────────────────────────────── */}
        <SectionDivider label="Лишь бы не засудили" className="mb-8" />

        <section className="mb-10 space-y-4">
          <p className="text-lg text-foreground leading-relaxed">
            {`Вопщем, теперь можно писать внутри статьи/персонажей {XdY+Z} или {XкY+Z}, где X, Y и Z числа (последнее может быть отрицательным)`}
          </p>
          <p className="text 2xl">
            Ну то есть примерно так:
            {` {2d6+2} атака, а для РуСсКиХ {4к3-2}`}, а в статье это
            отобразится как:
          </p>
          <Demo />
        </section>

        {/* ── Footer ───────────────────────────────────────────────────────── */}

        <p className="text-2xl italic mb-6">
          В целом все, сегодня патч относительно маленький.
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
