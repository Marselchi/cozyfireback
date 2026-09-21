package com.cozyfireplace.server.auth;


import com.cozyfireplace.server.auth.profile.Profile;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class UserDetailsImpl implements UserDetails {
    @Getter
    private final Profile profile;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Profile profile) {
        this.profile = profile;
        // Базовая роль для всех — для endpoints вне комнат
        this.authorities = AuthorityUtils.createAuthorityList("ROLE_USER");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return profile.getPassword();
    }

    @Override
    public String getUsername() {
        return profile.getUsername();
    }

    public static UserDetailsImpl build(Profile profile) {
        return new UserDetailsImpl(profile);
    }
}
