package com.cozyfireplace.server.templates.dto;

import com.cozyfireplace.server.templates.Template;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TemplateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "content", ignore = true)
    void updateTemplateFromRequest(TemplateUpdateRequest request, @MappingTarget Template template);

    TemplateResponse toTemplateResponse(Template template);

    TemplateDetailResponse toTemplateDetailResponse(Template template);
}
