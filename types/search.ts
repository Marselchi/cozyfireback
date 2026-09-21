import { IdName } from "./springTypes";

export interface LoreSearchParams {
  size: string | null;
  offset: string | null;
  query: string | null;
}

export interface LoreItemData {
  id: number;
  title: string;
  description: string;
  date: string;
  author: string;
  byAdmin: boolean;
  isViewed: boolean | null;
  nonPublic: boolean;
  tags: IdName[];
  /** Raw markdown content for fulltext search */
  content: string;
}

/** A single contiguous chunk of matched content returned by the backend.
 *  Match positions are encoded as \u0001…\u0002 sentinel pairs inside matchContent. */
export interface LoreMatchChunk {
  /** The Y-symbol context window. Hit spans are wrapped with \u0001 … \u0002. */
  matchContent: string;
  /** Number of times the query appears inside this matchContent chunk */
  occurrenceCount: number;
}

/** A search result item returned by the backend */
export interface LoreSearchResult {
  id: number;
  title: string;
  byAdmin: boolean;
  secret: boolean;
  /** Sum of occurrenceCount across ALL matches for this item (may exceed shown matches when notAll=true) */
  totalOccurrences: number;
  /** Whether the backend truncated the matches list (more matches exist beyond what's returned) */
  notAll: boolean;
  /** Ordered list of match chunks (each a Y-symbol window around grouped hits) */
  matches: LoreMatchChunk[];
  /** href target for navigating to the full lore item */
  href?: string;
}

/**
 * Paginated response envelope — offset-cursor style.
 * The client must never compute the next offset itself; always use nextOffset verbatim.
 */
export interface LoreSearchPage {
  /** Current page of results */
  content: LoreSearchResult[];
  /**
   * Pass this value as `offset` on the next request.
   * null means there is no next page — stop fetching.
   */
  nextOffset: number | null;
  /** Total matching Lore entries across all pages (not the count of loaded items) */
  totalElements: number;
}
