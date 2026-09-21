import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { ChangelogList } from "@/components/patchnotes/changelog-list";
import { CollapsibleSection } from "@/components/patchnotes/collapsible-section";
import { Callout } from "@/components/patchnotes/callout";
import { SectionDivider } from "@/components/patchnotes/section-divider";
import { ImageWithCaption } from "@/components/patchnotes/image-with-caption";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Block Reference — Patch Notes",
  description:
    "Every available content block in one place. Use this page as a template when writing new patchnotes.",
};

/**
 * Block Reference
 *
 * This page is a living template. Every block available for use in a
 * patchnote is shown here with a short annotation. Copy-paste what you need
 * into your new page.tsx and adjust the props.
 */
export default function V010Page() {
  return (
    <main className="min-h-screen bg-background">
      <div className="max-w-2/3 mx-auto px-6 py-10 md:py-16">
        {/* Back link */}
        <Link
          href="/patchnotes"
          className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors mb-10 group"
        >
          <ArrowLeft
            className="size-3.5 transition-transform duration-150 group-hover:-translate-x-0.5"
            aria-hidden="true"
          />
          К списку
        </Link>

        {/* ── Header ───────────────────────────────────────────────────────── */}
        <header className="mb-10">
          <div className="flex items-center gap-3 mb-4">
            <span className="font-mono text-xs font-semibold px-2.5 py-1 rounded-full bg-muted border border-input text-muted-foreground">
              v0.1.0
            </span>
            <span className="text-xs font-medium px-2.5 py-1 rounded-full bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300">
              Reference
            </span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground text-balance mb-2">
            Block Reference
          </h1>
          <time dateTime="2025-06-10" className="text-sm text-muted-foreground">
            June 10, 2025
          </time>
        </header>

        <Callout variant="info" title="How to use this page" className="mb-10">
          This is not a real release. It exists purely as a copy-paste
          reference. Each section below demonstrates one block and documents its
          props. To author a new patchnote, create{" "}
          <code className="font-mono text-xs bg-blue-100 dark:bg-blue-900/40 px-1 py-0.5 rounded">
            app/patchnotes/v1-0-0/page.tsx
          </code>
          , register it in{" "}
          <code className="font-mono text-xs bg-blue-100 dark:bg-blue-900/40 px-1 py-0.5 rounded">
            lib/patchnotes-registry.ts
          </code>
          , and import whatever blocks you need from{" "}
          <code className="font-mono text-xs bg-blue-100 dark:bg-blue-900/40 px-1 py-0.5 rounded">
            components/patchnotes/
          </code>
          .
        </Callout>

        {/* ── 1. Text & Headings ───────────────────────────────────────────── */}
        <SectionDivider label="1 — Text &amp; Headings" className="mb-8" />

        <section className="mb-10 space-y-4">
          <h2 className="text-xl font-bold tracking-tight text-foreground">
            Section heading (h2)
          </h2>
          <h3 className="text-base font-semibold text-foreground">
            Sub-section heading (h3)
          </h3>
          <p className="text-sm text-foreground leading-relaxed">
            Body copy lives in a{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              {"<p>"}
            </code>{" "}
            tag with{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              text-sm leading-relaxed
            </code>
            . This gives comfortable reading density without making paragraphs
            feel like a wall of text. Inline code is wrapped in a muted{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              {"<code>"}
            </code>{" "}
            element as shown here.
          </p>
          <p className="text-sm text-muted-foreground leading-relaxed">
            Secondary paragraphs use{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              text-muted-foreground
            </code>{" "}
            for visual hierarchy — useful for caveats, metadata, or supporting
            detail that should recede slightly from the primary narrative.
          </p>
        </section>

        {/* ── 2. Changelog List ────────────────────────────────────────────── */}
        <SectionDivider label="2 — Changelog List" className="mb-8" />

        <section className="mb-10">
          <p className="text-sm text-muted-foreground leading-relaxed mb-5">
            Pass an array of{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              {"{ type, items[] }"}
            </code>{" "}
            objects to{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              {"<ChangelogList groups={[...]} />"}
            </code>
            . Available types:{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              added
            </code>
            ,{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              changed
            </code>
            ,{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              fixed
            </code>
            ,{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              removed
            </code>
            .
          </p>
          <ChangelogList
            groups={[
              {
                type: "added",
                items: [
                  "Something brand new that wasn't there before.",
                  "A second new thing worth calling out.",
                ],
              },
              {
                type: "changed",
                items: ["An existing behaviour that now works differently."],
              },
              {
                type: "fixed",
                items: [
                  "A bug that caused intermittent failures under load.",
                  "An edge case in the parser that swallowed empty strings.",
                ],
              },
              {
                type: "removed",
                items: [
                  "A deprecated API that was announced for removal in the previous release.",
                ],
              },
            ]}
          />
        </section>

        {/* ── 3. Section Divider ───────────────────────────────────────────── */}
        <SectionDivider label="3 — Section Divider" className="mb-8" />

        <section className="mb-10 space-y-4">
          <p className="text-sm text-muted-foreground leading-relaxed">
            Plain divider: no props needed.
          </p>
          <SectionDivider />
          <p className="text-sm text-muted-foreground leading-relaxed">
            Labelled divider: pass a{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              label
            </code>{" "}
            string.
          </p>
          <SectionDivider label="Section label" />
        </section>

        {/* ── 4. Callout ───────────────────────────────────────────────────── */}
        <SectionDivider label="4 — Callout" className="mb-8" />

        <section className="mb-10 space-y-4">
          <p className="text-sm text-muted-foreground leading-relaxed">
            Four variants:{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              info
            </code>
            ,{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              tip
            </code>
            ,{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              warning
            </code>
            ,{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              danger
            </code>
            . The{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              title
            </code>{" "}
            prop is optional.
          </p>

          <Callout variant="info" title="Info">
            General information or context the reader should be aware of.
          </Callout>

          <Callout variant="tip" title="Tip">
            A helpful suggestion or best-practice recommendation.
          </Callout>

          <Callout variant="warning" title="Warning">
            Something that could go wrong if not handled carefully.
          </Callout>

          <Callout variant="danger" title="Danger">
            A destructive or irreversible operation — proceed with caution.
          </Callout>

          <Callout variant="info">
            Callout without a title — body text only.
          </Callout>
        </section>

        {/* ── 5. Collapsible Section ───────────────────────────────────────── */}
        <SectionDivider label="5 — Collapsible Section" className="mb-8" />

        <section className="mb-10 space-y-4">
          <p className="text-sm text-muted-foreground leading-relaxed">
            Wrap any content in{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              {'<CollapsibleSection label="...">'}
            </code>
            . Pass{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              defaultOpen
            </code>{" "}
            to start expanded. Works great around code blocks, long tables, or
            supplementary material.
          </p>

          <CollapsibleSection label="Example — collapsed by default">
            <pre className="overflow-x-auto p-4 text-[13px] leading-relaxed font-mono bg-[#0d1117] text-[#e6edf3]">
              <code>{`// components/patchnotes/collapsible-section.tsx
import { CollapsibleSection } from "@/components/patchnotes/collapsible-section"

// Basic usage
<CollapsibleSection label="Show code">
  <pre>...</pre>
</CollapsibleSection>

// Start open
<CollapsibleSection label="Show code" defaultOpen>
  <pre>...</pre>
</CollapsibleSection>`}</code>
            </pre>
          </CollapsibleSection>

          <CollapsibleSection label="Example — open by default" defaultOpen>
            <div className="p-4 text-sm text-muted-foreground leading-relaxed">
              Content does not have to be code. You can place any React nodes
              inside a collapsible — prose, tables, images, nested components.
              The container adds the border and the toggle; you own the interior
              layout entirely.
            </div>
          </CollapsibleSection>
        </section>

        {/* ── 6. Image with Caption ────────────────────────────────────────── */}
        <SectionDivider label="6 — Image with Caption" className="mb-8" />

        <section className="mb-10 space-y-4">
          <p className="text-sm text-muted-foreground leading-relaxed">
            Pass a{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              src
            </code>
            ,{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              alt
            </code>
            , and optionally a{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              caption
            </code>{" "}
            string. Remote images require a domain entry in{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              next.config.mjs
            </code>
            ; local images go in{" "}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded border border-border">
              public/
            </code>
            .
          </p>

          <ImageWithCaption
            src="/placeholder.svg?height=400&width=800"
            alt="Placeholder showing image dimensions"
            caption="Fig. 1 — A placeholder image. Replace src with your actual asset path."
            width={800}
            height={400}
          />
        </section>

        {/* ── Footer nav ───────────────────────────────────────────────────── */}
        <SectionDivider className="mb-8" />
        <Link
          href="/patchnotes"
          className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors group"
        >
          <ArrowLeft
            className="size-3.5 transition-transform duration-150 group-hover:-translate-x-0.5"
            aria-hidden="true"
          />
          Back to all patch notes
        </Link>
      </div>
    </main>
  );
}
