package com.cozyfireplace.server.lore.search;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bind e.g. in application.yml:
 * <p>
 * lore:
 * search:
 * context-chars: 80
 * max-chunk-size: 400
 * max-matches: 5
 */
@Data
@Component
@ConfigurationProperties(prefix = "lore.search")
public class LoreSearchProperties {

    /**
     * X: characters of context to grab before/after each raw match.
     */
    private int contextChars = 300;

    /**
     * Y: hard cap on a single chunk's length after merging.
     */
    private int maxChunkSize = 700;

    /**
     * Max number of chunks returned per Lore entry.
     */
    private int maxMatches = 10;
}

