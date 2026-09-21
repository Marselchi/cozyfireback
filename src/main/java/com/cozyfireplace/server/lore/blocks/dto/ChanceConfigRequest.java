package com.cozyfireplace.server.lore.blocks.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChanceConfigRequest {
    private String skill;
    private Integer threshold;
}
