package com.cozyfireplace.server.accounts.stats;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SkillMapper {

    SkillListResponse toSkillListResponse(Skill skill);
}
