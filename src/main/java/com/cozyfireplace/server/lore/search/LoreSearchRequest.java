package com.cozyfireplace.server.lore.search;

import lombok.*;

import java.util.List;

/**
 * Bundles the search endpoint's query params together with the resolved
 * viewer context (accountId / roleIds / canSeeAll), so the repository/service
 * signatures don't have to keep growing every time a filter is added.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class LoreSearchRequest {

    private String query;
    private Long roomId;
    private int size;
    private int offset;

    // optional filters
    private String title;
    private Boolean createdByCreator;
    private List<Long> tagIds;
    private String status; // "viewed" | "unviewed" | "updated"
}

