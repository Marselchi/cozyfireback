package com.cozyfireplace.server.lore.blocks.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NormalAccessRecordResponse {

    private Long id;
    private Long blockId;
    private Long userId;
    private String userName;
    private Boolean hasAccess;
}
