package com.cozyfireplace.server.accounts.dto;

public record AccountUpdateRoleRequest(Long oldRoleId, Long newRoleId) {
}
