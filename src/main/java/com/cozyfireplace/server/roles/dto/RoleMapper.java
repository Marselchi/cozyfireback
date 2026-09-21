package com.cozyfireplace.server.roles.dto;


import com.cozyfireplace.server.roles.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true)
    void updateRoleFromRequest(RoleUpdateRequest request, @MappingTarget Role role);

    RoleResponse toRoleResponse(Role role);

    @Mapping(target = "id", source = "role.id")
    @Mapping(target = "name", source = "role.name")
    @Mapping(target = "users", source = "userNames")
    RoleEditResponse toEditResponse(Role role, List<String> userNames);
}
