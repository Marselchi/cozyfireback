package com.cozyfireplace.server.accounts.stats;


import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillResponse {
    private String key;
    private String label;
    private String statKey;
    private Integer proficiency;
    private Integer modifier;
}
