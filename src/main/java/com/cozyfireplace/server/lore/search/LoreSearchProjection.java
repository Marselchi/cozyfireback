package com.cozyfireplace.server.lore.search;

/**
 * Flat projection returned by the native full-text search query.
 * Property names must match the SQL column aliases (case-insensitively) —
 * see {@link LoreSearchRepository}.
 */
public interface LoreSearchProjection {

    Long getId();

    String getTitle();

    Boolean getByAdmin();

    Boolean getSecret();

    /**
     * Full markdown content of the Lore. Kept raw here — occurrence counting,
     * chunking and markdown-safe trimming happen in the service layer (Part 2),
     * since they need the *processed* (role-restricted) content, not the raw column.
     */
    String getContent();
}

