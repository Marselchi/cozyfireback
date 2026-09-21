import { ArrowLeft } from "lucide-react";
import { ChangelogList } from "@/components/patchnotes/changelog-list";
import { CollapsibleSection } from "@/components/patchnotes/collapsible-section";
import { Callout } from "@/components/patchnotes/callout";
import { SectionDivider } from "@/components/patchnotes/section-divider";
import { ImageWithCaption } from "@/components/patchnotes/image-with-caption";
import type { Metadata } from "next";
import CustomLink from "@/components/no-prefetch-link";

export const metadata: Metadata = {
  title: "v1.4.0 — Скрытые блоки, полнотекстовый поиск и лист персонажа",
  description: "Объем: пизда. Статус разраба: нет.",
};

export default function V140Page() {
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
              v2.0.0
            </span>
            <span className="text-xs font-medium px-2.5 py-1 rounded-full bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300">
              Объем: пизда
            </span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground text-balance mb-2">
            Статус разраба: нет
          </h1>
          <time dateTime="2026-09-18" className="text-sm text-muted-foreground">
            18 сентября, 2026
          </time>
        </header>

        {/* ── 1. Скрытые блоки и полнотекстовый поиск ─────────────────────── */}
        <SectionDivider
          label="Нахуя я сюда полез: Скрытые блоки и полнотекстовый поиск"
          className="mb-8"
        />

        <section className="mb-10 space-y-4">
          <p className="text-sm text-foreground leading-relaxed">
            Скрытые блоки наконец-то перестали быть внутри контента статьи и
            теперь хранятся как полноценные отдельные сущности. Очень классно
            круто молодежно ахуенно, но полнотекстовый поиск теперь шарится по 6
            связанным сущностям. Это просто жуть, никогда больше блять никогда.
          </p>

          <ChangelogList
            groups={[
              {
                type: "added",
                items: [
                  "Скрытые блоки — теперь отдельные сущности, а не кусок текста внутри статьи",
                  "Новая фича: теперь в скрытых блоках можно ролить d20. Управление роллами в админке можете глянуть авторы комнат.",
                ],
              },
              {
                type: "changed",
                items: [
                  "Полнотекстовый поиск переработан под новую структуру блоков (спасибо, что живой)",
                ],
              },
            ]}
          />

          <ImageWithCaption
            src="/rollable_dialog.webp"
            alt="Место для скриншота: интерфейс ролла d20 или админская панель"
            caption="Место для скриншота: интерфейс ролла d20"
            width={400}
            height={400}
          />

          <CollapsibleSection label="Почему это было больно">
            <div className="p-4 text-sm leading-relaxed space-y-3">
              <p>
                Скрытые блоки жили прямо внутри markdown статьи, а теперь нихуя
                блять свой id, свои роли.
              </p>
              <p>
                P.s частичную дубликацию результатов полнотекста уберу в некст
                патче.
              </p>
            </div>
          </CollapsibleSection>
        </section>

        {/* ── 2. Лист персонажа ────────────────────────────────────────────── */}
        <SectionDivider label="Лист персонажа" className="mb-8" />

        <section className="mb-10 space-y-4">
          <p className="text-sm text-foreground leading-relaxed">
            Нахуя? Для ролов. Стоило того? Нет. Спасити.
          </p>
          <p className="text-sm text-foreground leading-relaxed">
            Можно импортировать персонажа напрямую с LSS — просто качаете JSON
            своего перса и закидываете его в форму импорта.
          </p>

          <ChangelogList
            groups={[
              {
                type: "added",
                items: [
                  "Импорт персонажа из JSON-экспорта LSS (пока только D&D 5e)",
                ],
              },
            ]}
          />

          <ImageWithCaption
            src="/account_self.webp"
            alt="Место для скриншота"
            caption="Место для скриншота навигации"
            width={400}
            height={400}
          />
        </section>

        {/* ── 3. Невидимая оптимизация ─────────────────────────────────────── */}
        <SectionDivider label="Невидимая оптимизация" className="mb-8" />

        <section className="mb-10 space-y-4">
          <p className="text-sm text-foreground leading-relaxed">
            Пока я не сошел с ума от рефакторинга, я успел вычистить и ускорить
            кучу мелких деталей по всему сайту. Удержал себя и не пошел на
            очередной рефакторинг редактора — слава господу богу.
          </p>

          <Callout variant="tip">
            Ничего конкретного показать не могу но вы заметите скорее всего.
          </Callout>
        </section>

        {/* ── Future plans ─────────────────────────────────────────────────── */}
        <SectionDivider label="Чо дальше" className="mb-8" />

        <section className="mb-10 space-y-4">
          <ChangelogList
            groups={[
              {
                type: "planned",
                items: [
                  "Больше систем импорта персонажей",
                  "Продолжение борьбы с полнотекстовым поиском",
                  "Бесконечный фикс багов",
                ],
              },
            ]}
          />
        </section>

        {/* ── Outro / Footer ───────────────────────────────────────────────── */}
        <SectionDivider label="Аутро" className="mb-8" />

        <section className="mb-10 space-y-4">
          <p className="text-sm text-foreground leading-relaxed">
            Идите тестировать, находить баги — их должно быть ДОХУЯ. А я пойду
            нахуй. Или плакать. Пока не решил.
          </p>
        </section>

        <p className="text-2xl italic mb-6">
          Всем бб (бекап на всякий сделал, тесты на проде — лучшие)
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
