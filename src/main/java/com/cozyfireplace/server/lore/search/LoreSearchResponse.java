package com.cozyfireplace.server.lore.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoreSearchResponse {

    private Long id;

    private String title;

    private boolean byAdmin;

    private boolean secret;

    /** Total number of query occurrences found in the (role-processed) content. */
    private int totalOccurrences;

    /** True if matches were truncated to maxMatches, i.e. not all occurrences are shown. */
    private boolean notAll;

    private List<MatchDto> matches;
}
