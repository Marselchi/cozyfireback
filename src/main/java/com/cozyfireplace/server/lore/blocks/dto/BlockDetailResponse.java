package com.cozyfireplace.server.lore.blocks.dto;

import com.cozyfireplace.server.lore.dto.IdName;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockDetailResponse {
    private Long id;
    private Long loreId;
    private String title;
    private String content;
    private String type;
    private List<String> roles;
    private ChanceConfigResponse chance;
}
