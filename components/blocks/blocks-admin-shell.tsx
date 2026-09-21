"use client";

import { useEffect, useState } from "react";
import { BlockListPanel } from "./block-list-panel";
import { SelectedBlockPanel } from "./selected-block-panel";

export function BlocksAdminShell() {
  const [selectedId, setSelectedId] = useState<string | null>(null);

  // BlockListPanel/SelectedBlockPanel use useSuspenseInfiniteQuery / useSuspenseQuery
  // hitting relative "/api/..." routes. During SSR (this component is a Client
  // Component, but Client Components still render on the server for the initial
  // HTML) that fetch has no origin to resolve against and throws
  // ("Invalid URL '/api/blocks/1?...'"), which is why Next fell back to
  // client-only rendering with a console error. Delaying the data-fetching
  // subtree until after mount means that fetch simply never runs on the server.
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);

  if (!mounted) {
    return <div className="h-full grid grid-cols-1 md:grid-cols-[320px_1fr]" />;
  }

  return (
    // h-full instead of flex-1: this component isn't guaranteed to sit inside a
    // flex parent, and flex-1 with no bounded ancestor height just lets the grid
    // grow to fit its content (which is what was making the whole page taller
    // whenever new blocks loaded). h-full inherits the bounded height set up by
    // the admin layout + page wrapper instead.
    <div className="grid h-full grid-cols-1 overflow-hidden md:grid-cols-[320px_1fr]">
      <aside className="flex min-h-0 flex-col overflow-hidden border-b border-border md:border-b-0 md:border-r">
        <BlockListPanel selectedId={selectedId} onSelect={setSelectedId} />
      </aside>
      <section className="min-h-0 overflow-hidden">
        <SelectedBlockPanel blockId={selectedId} />
      </section>
    </div>
  );
}
