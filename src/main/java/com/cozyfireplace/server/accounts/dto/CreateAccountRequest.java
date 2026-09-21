package com.cozyfireplace.server.accounts.dto;

import lombok.Value;

@Value
public class CreateAccountRequest {
    String username;
    String invitationCode;
}
