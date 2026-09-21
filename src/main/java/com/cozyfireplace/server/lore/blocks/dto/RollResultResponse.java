package com.cozyfireplace.server.lore.blocks.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RollResultResponse {
    private Long id;
    private Integer rollValue;
    private Integer modifier;
    private Integer threshold;
    private String skill;
    private Boolean passed;
}
