package com.cozyfireplace.server.lore.blocks.dto;

import com.cozyfireplace.server.lore.blocks.AccessStatus;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessRecordResponse {
    private Long id;
    private Long blockId;
    private Long userId;
    private String userName;
    private String status;
    private Integer rollValue;
    private Boolean passed;

    public AccessRecordResponse(
            Long id,
            Long blockId,
            Long userId,
            String userName,
            AccessStatus status,
            Integer rollValue,
            Boolean passed
    ) {
        this.id = id;
        this.blockId = blockId;
        this.userId = userId;
        this.userName = userName;
        this.status = status != null ? status.name() : null;
        this.rollValue = rollValue;
        this.passed = passed;
    }
}
