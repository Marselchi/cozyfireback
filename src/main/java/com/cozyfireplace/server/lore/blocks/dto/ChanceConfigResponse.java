package com.cozyfireplace.server.lore.blocks.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChanceConfigResponse {
    private String skill;
    private Integer threshold;
}
