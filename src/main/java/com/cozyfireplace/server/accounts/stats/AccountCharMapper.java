package com.cozyfireplace.server.accounts.stats;


import com.cozyfireplace.server.accounts.AccountChar;
import org.mapstruct.*;

import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountCharMapper {

    CharacterResponse toResponse(AccountChar accountChar);

    @AfterMapping
    default void sortStatsAndSkills(AccountChar accountChar, @MappingTarget CharacterResponse response) {
        List<StatBlockResponse> sortedStats = accountChar.getStats().stream()
                .sorted(Comparator.comparing(
                        s -> s.getStat().getSortOrder(),
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(this::toStatBlockResponse)
                .toList();
        response.setStats(sortedStats);

        List<SkillResponse> sortedSkills = accountChar.getSkills().stream()
                .sorted(Comparator.comparing(
                        s -> s.getSkill().getSortOrder(),
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(this::toSkillResponse)
                .toList();
        response.setSkills(sortedSkills);
    }

    default StatBlockResponse toStatBlockResponse(AccountCharStat stat) {
        if (stat == null) return null;
        return StatBlockResponse.builder()
                .key(stat.getStat().getKey())
                .label(stat.getStat().getLabel())
                .score(stat.getScore())
                .modifier(stat.getModifier())
                .build();
    }

    default SkillResponse toSkillResponse(AccountCharSkill skill) {
        if (skill == null) return null;
        return SkillResponse.builder()
                .key(skill.getSkill().getKey())
                .label(skill.getSkill().getLabel())
                .statKey(skill.getSkill().getStat() != null ? skill.getSkill().getStat().getKey() : null)
                .proficiency(skill.getProficiency())
                .modifier(skill.getModifier())
                .build();
    }
}