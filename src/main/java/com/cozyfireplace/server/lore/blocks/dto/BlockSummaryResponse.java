package com.cozyfireplace.server.lore.blocks.dto;

import com.cozyfireplace.server.lore.dto.IdName;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockSummaryResponse {
    private Long id;
    private Long loreId;
    private String title;
    private String type;
    private String contentPreview;
    private Boolean hasChance;
    private List<String> roles;
}
