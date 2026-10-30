package com.cozyfireplace.server.auth;

import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.auth.profile.ProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CustomUserDetailsService} and the {@link UserDetailsImpl} it produces.
 * <p>
 * The repository is mocked. Covers the found/not-found branches of the lookup and asserts the
 * mapping into {@link UserDetailsImpl}: credential/username delegation to the profile plus the
 * default {@code ROLE_USER} authority.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    @DisplayName("wraps the found profile in a UserDetailsImpl")
    void found() {
        Profile profile = Profile.builder().username("user").password("hash").build();
        when(profileRepository.findByUsername("user")).thenReturn(Optional.of(profile));

        UserDetails details = service.loadUserByUsername("user");

        assertThat(details).isInstanceOf(UserDetailsImpl.class);
        assertThat(((UserDetailsImpl) details).getProfile()).isSameAs(profile);
        assertThat(details.getUsername()).isEqualTo("user");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("throws when the username is unknown")
    void notFound() {
        when(profileRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
