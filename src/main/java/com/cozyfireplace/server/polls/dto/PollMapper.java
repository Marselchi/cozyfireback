package com.cozyfireplace.server.polls.dto;


import com.cozyfireplace.server.polls.Poll;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PollMapper {
    @Mapping(target = "profileName", source = "poll.profile.username")
    @Mapping(target = "profileEmail", source = "poll.profile.email")
    PollResponse toPollResponse(Poll poll);
}
