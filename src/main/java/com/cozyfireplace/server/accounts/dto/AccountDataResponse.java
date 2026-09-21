package com.cozyfireplace.server.accounts.dto;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.roles.dto.RoleResponse;
import lombok.Builder;
import lombok.Value;

import java.util.Set;
import java.util.stream.Collectors;

@Builder
@Value
public class AccountDataResponse {
    Long id;
    String username;
    Set<RoleResponse> roles;
    boolean isAdmin;

    public static AccountDataResponse from(Account account, boolean isCreator) {
        return AccountDataResponse.builder()
                .username(account.getName())
                .id(account.getId())
                .roles(account.getRoles().stream()
                        .map(role -> RoleResponse.builder()
                                .id(role.getId())
                                .name(role.getName())
                                .build()
                        )
                        .collect(Collectors.toSet())
                )
                .isAdmin(isCreator)
                .build();
    }
}
