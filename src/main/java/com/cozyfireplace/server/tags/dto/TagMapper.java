package com.cozyfireplace.server.tags.dto;


import com.cozyfireplace.server.tags.Tag;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TagMapper {
    //TODO: toEntity, toDto and updateEntity for all dtos
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true)
    Tag toEntity(TagCreateRequest tagCreateRequest);

    TagCreateRequest toDto(Tag tag);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true)
    void updateEntity(TagUpdateRequest request, @MappingTarget Tag tag);

    TagResponse toResponse(Tag tag);
}
