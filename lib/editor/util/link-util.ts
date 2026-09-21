const APP_DOMAIN = "cozyfireplace.ru";

// Matches "/rooms/{roomName}/lore/{loreId}#{heading}" — the heading is
// required, since a bare lore link (no #heading) should just navigate
// normally rather than trigger a popup.
const LORE_HEADING_PATTERN = /^\/rooms\/([^/]+)\/lore\/([^/#]+)#(.+)$/;

export const LINK_BASE_CLASS =
  "text-blue-600 dark:text-blue-400 underline hover:text-blue-800 dark:hover:text-blue-300 cursor-pointer";

export type LoreTarget = { roomName: string; loreId: string; heading: string };

export type LinkClassification = {
  isInternal: boolean;
  href?: string;
  lore?: LoreTarget;
};

export function classifyLink(url: string): LinkClassification {
  let parsed: URL;
  let isInternal: boolean;

  if (url.startsWith("/")) {
    // Relative URL — always internal to this app. Parse against a dummy
    // base purely to get consistent pathname/search/hash extraction.
    isInternal = true;
    try {
      parsed = new URL(url, `https://${APP_DOMAIN}`);
    } catch {
      return { isInternal: false };
    }
  } else {
    try {
      parsed = new URL(url);
    } catch {
      // Not a parseable absolute URL (mailto:, malformed, etc.) — external.
      return { isInternal: false };
    }

    const normalizeHost = (h: string) => h.replace(/^www\./, "");
    isInternal =
      !!APP_DOMAIN &&
      normalizeHost(parsed.hostname) === normalizeHost(APP_DOMAIN);
  }

  if (!isInternal) return { isInternal: false };

  // Always relative: this is what actually gets handed to next/link.
  const href = `${parsed.pathname}${parsed.search}${parsed.hash}`;
  const hash = parsed.hash.replace(/^#/, "");

  const match = new RegExp(LORE_HEADING_PATTERN).exec(
    `${parsed.pathname}${hash ? `#${hash}` : ""}`,
  );
  if (!match) return { isInternal: true, href };

  const [, roomName, loreId, heading] = match;
  return { isInternal: true, href, lore: { roomName, loreId, heading } };
}

export function linkClassName(isInternal: boolean, hasLore: boolean): string {
  return [hasLore ? "lore-link" : "", isInternal && "internal-link"]
    .filter(Boolean)
    .join(" ");
}
