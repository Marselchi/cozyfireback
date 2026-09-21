package com.cozyfireplace.server.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class SignupRequest {
    @NotBlank
    @Size(min = 3, max = 16)
    String username;
    @NotBlank @Email
    String email;
    @NotBlank @Size(min = 6, max = 20)
    String password;
    @NotBlank
    String code;
}
