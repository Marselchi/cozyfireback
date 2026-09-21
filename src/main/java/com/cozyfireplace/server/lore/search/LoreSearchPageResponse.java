package com.cozyfireplace.server.lore.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Offset-paginated search response.
 *
 * nextOffset is null when there is no more data to fetch (end of results);
 * it is NOT simply offset + content.size(), because secondary/content-level
 * filtering in the service layer may consume more DB rows than valid results
 * returned — see Part 2 for the recovery logic that computes this value.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoreSearchPageResponse {

    private List<LoreSearchResponse> content;

    private Integer nextOffset;

    private Long totalElements;
}


