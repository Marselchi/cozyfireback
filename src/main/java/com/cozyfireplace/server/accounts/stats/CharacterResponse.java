package com.cozyfireplace.server.accounts.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterResponse {
    private Long id;
    private String name;
    private Integer level;
    private String characterClass;
    private String race;
    private String origin;
    private Integer version;
    private List<StatBlockResponse> stats;
    private List<SkillResponse> skills;
}