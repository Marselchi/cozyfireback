package com.cozyfireplace.server.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class LoginRequest {
    @NotBlank(message = "Username is required")
    String username;
    @NotBlank(message = "Password is required")
    String password;
}
