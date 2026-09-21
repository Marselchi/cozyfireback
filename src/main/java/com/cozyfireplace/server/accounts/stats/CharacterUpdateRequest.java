package com.cozyfireplace.server.accounts.stats;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterUpdateRequest {
    private String name;
    private Integer level;
    private String characterClass;
    private String race;
    private GameSystem system;
    private String origin;
    private Integer version;
    private List<StatBlockResponse> stats;
    private List<SkillResponse> skills;
}
