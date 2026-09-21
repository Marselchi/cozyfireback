package com.cozyfireplace.server.lore.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchDto {

    private String matchContent;

    private int occurrenceCount;
}

