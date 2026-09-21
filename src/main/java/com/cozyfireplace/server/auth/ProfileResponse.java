package com.cozyfireplace.server.auth;

import com.cozyfireplace.server.auth.profile.Profile;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ProfileResponse {
    String username;
    String email;
    Long id;
    public static ProfileResponse from(Profile profile) {
        return ProfileResponse.builder()
                .username(profile.getUsername())
                .email(profile.getEmail())
                .id(profile.getId())
                .build();
    }
}
