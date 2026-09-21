package com.cozyfireplace.server.auth.security;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class JwtAuthResponse {
    String accessToken;
}
