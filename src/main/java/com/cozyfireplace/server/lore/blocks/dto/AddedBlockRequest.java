package com.cozyfireplace.server.lore.blocks.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddedBlockRequest {
    private String localId;
    private String type;
    private String content;
    private Map<String, Object> metadata;
}
