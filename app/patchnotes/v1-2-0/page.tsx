import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { ChangelogList } from "@/components/patchnotes/changelog-list";
import { CollapsibleSection } from "@/components/patchnotes/collapsible-section";
import { Callout } from "@/components/patchnotes/callout";
import { SectionDivider } from "@/components/patchnotes/section-divider";
import type { Metadata } from "next";
import CustomLink from "@/components/no-prefetch-link";
import { ImageWithCaption } from "@/components/patchnotes/image-with-caption";

export const metadata: Metadata = {
  title: "v1.2.0 — Patch Notes",
  description: "Поганые таблицы теперь работают (вроде)",
};

export default function V120Page() {
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
              v1.2.0
            </span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground text-balance mb-2">
            Мама можно мне Excel? У нас уже есть Excel дома. Excel дома:
          </h1>
          <time dateTime="2026-07-11" className="text-sm text-muted-foreground">
            11 июля, 2026
          </time>
        </header>

        <Callout variant="info" className="mb-10">
          <p>
            Работа с таблицами возможна ток через менюшку с правой кнопки мыши.
            В верхнюю добавлю потом... когда нибудь
          </p>
          <p>
            Если чето сломается - пишите <strong>Матвею</strong>, у меня
            двухдневный отпуск. Он должен справится я в него верю. Как раз
            говорил что хочет сайты научиться делать
          </p>
        </Callout>

        {/* ── Main changes ─────────────────────────────────────────────────── */}
        <SectionDivider label="АНЕТ ТЫ ВСЕ СЛОМАЛ" className="mb-8" />

        <section className="mb-10 space-y-4">
          <p className="text-sm text-foreground leading-relaxed">
            Благодаря зорким глазам Андрея - я обнаружил что забыл добавить
            правило для парсинга и рендера таблиц. Ну а там между делом решил и
            нормальную работу с таблицами добавить
          </p>
        </section>

        {/* ── Future plans ─────────────────────────────────────────────────── */}
        <SectionDivider label="Чо дальше" className="mb-8" />

        <section className="mb-10 space-y-4">
          <p className="text-sm text-foreground leading-relaxed mb-4">
            Этот нежданчик чутка спутал мои планы, но в целом остается как есть,
            с добавлением одного пункта
          </p>

          <ChangelogList
            groups={[
              {
                type: "planned",
                items: [
                  "Полнотекстовый поиск",
                  "Поддержка изображений",
                  "Улучшения UI — он мне перестал нравиться, немного подлатаю",
                  "!Улучшение тулбара!",
                  "Бесконечный фикс багов",
                ],
              },
            ]}
          />

          <Callout variant="tip" className="mt-6">
            Я НЕ УВЕРЕН ЧТО ЗАЛАТАЛ ВСЕ СПОСОБЫ ВСТАВИТЬ СПОЙЛЕР В ТАБЛИЦУ.
            ПОЖАЛУЙСТА НЕ ВСТАВЛЯЙТЕ СПОЙЛЕР В ТАБЛИЦУ!!!!!!!!!
          </Callout>
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
