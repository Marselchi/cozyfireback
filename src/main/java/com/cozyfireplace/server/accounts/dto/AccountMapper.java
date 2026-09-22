package com.cozyfireplace.server.accounts.dto;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.roles.dto.RoleResponse;
import org.mapstruct.*;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    @Mapping(target = "roles", source = "account.roles")
    @Mapping(target = "isAdmin", source = "isCreator")
    @Mapping(target = "username", source = "account.name")
    AccountDataResponse toAccountDataResponse(Account account, boolean isCreator);

    @Mapping(target = "username", source = "account.name")
    @Mapping(target = "profileName", source = "account.profile.username")
    RoomAccountResponse toRoomAccountResponse(Account account);

    default Set<RoleResponse> mapRoles(Set<Role> roles) {
        return roles != null ?
                roles.stream()
                        .map(this::toRoleResponse)
                        .collect(Collectors.toSet()) :
                Collections.emptySet();
    }

    RoleResponse toRoleResponse(Role role);

    Account toEntity(Account account);

    @Mapping(target = "username", source = "account.name")
    @Mapping(target = "id", source = "account.id")
    AccountQuestionDataResponse toAccountQuestionDataResponse(Account account);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "accountChar", ignore = true)
    Account updateSelf(AccountUpdateSelfRequest request, @MappingTarget Account account);
}
