package com.cozyfireplace.server.accounts.stats;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatBlockResponse {
    private String key;
    private String label;
    private Integer score;
    private Integer modifier;
}