package com.cozyfireplace.server.lore.blocks.dto;


import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpandedBlockResponse {
    private Long id;
    private String type;
    private String content;
    private List<String> roles;
    private ChanceConfigResponse chance;
}
