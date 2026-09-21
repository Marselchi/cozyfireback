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
  title: "v1.1.0 — Patch Notes",
  description:
    "Первый крупный патч после защиты диплома. Полная переработка архитектуры.",
};

export default function V110Page() {
  return (
    <main className="min-h-screen bg-background">
      <div className="max-w-2/3 mx-auto px-6 py-10 md:py-16">
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
              v1.1.0
            </span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground text-balance mb-2">
            Полная переработка архитектуры
          </h1>
          <time dateTime="2026-07-08" className="text-sm text-muted-foreground">
            8 июля, 2026
          </time>
        </header>

        <Callout variant="info" className="mb-10">
          Чтож, первый достаточно крупный патч после того как я защитил диплом.
          Для вас как пользователей — по большей части ничего нового (кроме
          того, что я бы на вашем месте удалил старые черновики). Я хотел в этот
          же патч уместить полнотекстовый поиск, но он гига костылевый, так что
          пока нет )))
        </Callout>

        {/* ── Main changes ─────────────────────────────────────────────────── */}
        <SectionDivider label="Что изменилось" className="mb-8" />

        <section className="mb-10 space-y-4">
          <p className="text-sm text-foreground leading-relaxed">
            Под капотом поменялось <strong>ВСЁ</strong>. Теперь всё гораздо
            удобнее, и к тому же быстрее :)
          </p>

          <ChangelogList
            groups={[
              {
                type: "changed",
                items: [
                  "Полная переработка архитектуры парсинга markdown",
                  "Все типы markdown теперь хранятся в отдельных файлах как ноды",
                  "Ручная реализация парсинга вместо готовых решений",
                ],
              },
            ]}
          />
        </section>

        {/* ── Technical details ────────────────────────────────────────────── */}
        <SectionDivider label="Технические детали" className="mb-8" />

        <section className="mb-10 space-y-4">
          <p className="text-sm leading-relaxed">
            Кому интересна техническая часть:
          </p>

          <CollapsibleSection label="Подробнее о парсинге">
            <div className="p-4 text-sm leading-relaxed space-y-3">
              <p>
                До этого парсинг был захардкоден по 5 или около того файлам, и
                каждое изменение было КРАЙНЕ болезненным. Поэтому ничего нового
                добавлять не хотелось, сейчас это изменилось.
              </p>
              <p>
                Теперь все типы markdown хранятся в отдельных файлах как ноды, и
                парсинг тоже происходит ручками. Это был ужас, но зато теперь
                всё работает быстрее и гибче.
              </p>
              <CollapsibleSection label="Архитектура">
                <pre className="overflow-x-auto p-4 text-[13px] leading-relaxed font-mono bg-[#0d1117] text-[#e6edf3]">
                  <code>{`
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│                              MARKDOWN STRING                                │
│                                                                             │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                          markdown-it (Tokenizer)                           │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │  Plugins:                                                            │  │
│  │   • inline-editor   [//]: # (vsbS:[...])                             │  │
│  │   • restricted-block  ::: restrictedBlock                            │  │
│  │   • quote-block     <? text                                          │  │
│  │   • placeholder     {{...}}                                          │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────┬─────────────────────────────────────────┘
                                   │
                                   ▼
                        ┌─────────────────────┐
                        │   Token Stream      │
                        │   Token[]           │
                        └──────────┬──────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Registry + ParserState                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  Парсеры (match → parse):                                            │   │
│  │   paragraph • heading • blockquote • code • list • image             │   │
│  │   inline-editor • restricted-block • quote-block • text (inline)     │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  parseMarkdown(markdown): Descendant[]                                      │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
                                   ▼
                        ┌─────────────────────┐
                        │   Descendant[]      │
                        │   (Slate AST)       │
                        └──────────┬──────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
              ▼                    ▼                    ▼
┌─────────────────────┐ ┌─────────────────────┐ ┌─────────────────────┐
│   React Renderer    │ │  Slate Editor       │ │   Serialize         │
│   (просмотр)        │ │  (parsed mode)      │ │   (save)            │
│                     │ │                     │ │                     │
│  Element:           │ │  withInlineEditors  │ │ serializeToMarkdown │
│   • paragraph       │ │  withShortcuts      │ │         ↓           │
│   • heading         │ │  withBlockEnter     │ │  markdown string    │
│   • link            │ │                     │ │                     │
│   • image           │ │  + Raw mode:        │ │ serializePreview    │
│   • inline-editor   │ │    plain text       │ │  (restrictedBlock)  │
│                     │ │    editor           │ │                     │
│  Leaf:              │ │                     │ │                     │
│   • bold, italic    │ │                     │ │                     │
│   • code, strike    │ │                     │ │                     │
│                     │ │                     │ │                     │
└─────────────────────┘ └─────────────────────┘ └─────────────────────┘
              │                    │                    │
              ▼                    ▼                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│                           RENDERED OUTPUT                                   │
│                        (HTML / Markdown / UI)                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                  `}</code>
                </pre>
              </CollapsibleSection>
            </div>
            <ImageWithCaption
              src="/patchnote110.png?height=400&width=800"
              alt="Количество боли"
              caption="Это было отвратительно"
              width={800}
              height={400}
            />
          </CollapsibleSection>
        </section>

        {/* ── Future plans ─────────────────────────────────────────────────── */}
        <SectionDivider label="Планы на будущее" className="mb-8" />

        <section className="mb-10 space-y-4">
          <p className="text-sm text-foreground leading-relaxed mb-4">
            Из ближайшего:
          </p>

          <ChangelogList
            groups={[
              {
                type: "planned",
                items: [
                  "Полнотекстовый поиск",
                  "Поддержка изображений",
                  "Улучшения UI — он мне перестал нравиться, немного подлатаю",
                  "Бесконечный фикс багов",
                ],
              },
            ]}
          />

          <Callout variant="tip" className="mt-6">
            Если находите баги — пишите мне, постараюсь починить.
          </Callout>
          <Callout variant="warning" className="mt-6">
            <p>Я знаю о баге где:</p>
            <p>{`> Цитата, а за ней`}</p>
            <p>просто текст</p>

            <p>
              При переключении режимов кушается внутрь цитаты. Чинить лень потом
              починю. Плюс цитату нельзя вырубить в рендер режиме. Если кому-то
              прям ПОЗАРЕЗ надо, скажите сделаю.
            </p>
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
