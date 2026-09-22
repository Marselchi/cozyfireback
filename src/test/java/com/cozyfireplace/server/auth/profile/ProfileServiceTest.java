package com.cozyfireplace.server.auth.profile;

import com.cozyfireplace.server.auth.UserDetailsImpl;
import com.cozyfireplace.server.util.exception.ForbiddenException;
import com.cozyfireplace.server.util.exception.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProfileService}.
 * <p>
 * The service reads the caller from {@link SecurityContextHolder} (no collaborators), so the
 * tests populate the context directly. Covers the not-authenticated guard, the wrong-principal
 * type guard and the happy path returning the bound profile.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService")
class ProfileServiceTest {

    @InjectMocks
    private ProfileService service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("throws Forbidden when nothing is authenticated")
    void noAuthentication() {
        assertThatThrownBy(() -> service.getCurrentProfile())
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("throws Forbidden for an unauthenticated token")
    void unauthenticated() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "pw"));

        assertThatThrownBy(() -> service.getCurrentProfile())
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("throws NotFound when the principal is not a UserDetailsImpl")
    void wrongPrincipalType() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("raw-string", "pw",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThatThrownBy(() -> service.getCurrentProfile())
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("returns the profile wrapped by the authenticated principal")
    void returnsProfile() {
        Profile profile = Profile.builder().username("user").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(UserDetailsImpl.build(profile), "pw",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThat(service.getCurrentProfile()).isSameAs(profile);
    }
}
