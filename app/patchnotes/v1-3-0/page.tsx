import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { ChangelogList } from "@/components/patchnotes/changelog-list";
import { CollapsibleSection } from "@/components/patchnotes/collapsible-section";
import { Callout } from "@/components/patchnotes/callout";
import { SectionDivider } from "@/components/patchnotes/section-divider";
import type { Metadata } from "next";
import CustomLink from "@/components/no-prefetch-link";
import { ImageWithCaption } from "@/components/patchnotes/image-with-caption";
import { FeedbackForm } from "@/components/patchnotes/feedback";

export const metadata: Metadata = {
  title: "v1.3.0 — ПОЛНОТЕКСТНЫЙ ЙИИИИИХАУ",
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
              v1.3.0
            </span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground text-balance mb-2">
            Это был полный ужас...
          </h1>
          <time dateTime="2026-07-19" className="text-sm text-muted-foreground">
            19 июля, 2026
          </time>
        </header>

        {/* ── Main changes ─────────────────────────────────────────────────── */}
        <SectionDivider label="УРАААААА" className="mb-8" />

        <section className="mb-10 space-y-4">
          <p className="text-sm text-foreground leading-relaxed">
            Штош, теперь на странице лора появилась кнопочка для поиска по
            содержимому лора... Писать это было просто. Я хотел тут побухтеть на
            всё это, но насток устал что обойдемся без этого. Наслаждайтесь
          </p>
        </section>

        {/* ── Future plans ─────────────────────────────────────────────────── */}
        <SectionDivider label="Прикольная штука" className="mb-8" />

        <section className="mb-10 space-y-4">
          <p className="text-sm text-foreground leading-relaxed mb-4">
            Помимо этого, еще сделал небольшую штукенцию: вы теперь можете
            оставлять фидбек/предложения, например вот такое:
          </p>
          <div className="flex justify-center">
            <FeedbackForm
              source="suggestion"
              title="Хотите предложить новую фичу?"
              description="Ну так предлагай йоу."
              placeholder="Пиши пиши давай..."
              buttonText="И отправляй"
              className="w-full"
            />
          </div>
        </section>

        <SectionDivider label="Так что пользуясь случаем..." className="mb-8" />

        <section className="mb-10 space-y-4">
          <div className="flex justify-center">
            <FeedbackForm
              source="can-help"
              title="Если вы хотите помочь с сайтом"
              description="Напишите свою почту, открою репу вам."
              placeholder="Пиши пиши давай..."
              buttonText="Спасити"
              className="w-full"
            />
          </div>
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
