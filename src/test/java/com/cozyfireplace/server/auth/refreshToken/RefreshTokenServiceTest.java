package com.cozyfireplace.server.auth.refreshToken;

import com.cozyfireplace.server.auth.profile.Profile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RefreshTokenService}.
 * <p>
 * The repository is mocked. Covers token creation (active flag, random token, future expiry and
 * profile binding), the active-token lookup, single and bulk revocation, and the validity check
 * driven purely by the expiry timestamp.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService service;

    @Nested
    @DisplayName("createRefreshToken")
    class Create {

        @Test
        @DisplayName("issues an active token with a future expiry bound to the profile")
        void issuesActiveToken() {
            Profile profile = Profile.builder().username("user").build();
            RefreshToken saved = RefreshToken.builder().id(1L).build();
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(saved);

            RefreshToken result = service.createRefreshToken(profile, 3600);

            assertThat(result).isSameAs(saved);
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            RefreshToken created = captor.getValue();
            assertThat(created.getProfile()).isSameAs(profile);
            assertThat(created.isActive()).isTrue();
            assertThat(created.getToken()).isNotBlank();
            assertThat(created.getExpiryDate()).isAfter(Instant.now());
        }
    }

    @Test
    @DisplayName("findByToken looks up only active tokens")
    void findByToken() {
        RefreshToken token = mock(RefreshToken.class);
        when(refreshTokenRepository.findByTokenAndActiveTrue("t")).thenReturn(Optional.of(token));

        assertThat(service.findByToken("t")).contains(token);
    }

    @Nested
    @DisplayName("revoke")
    class Revoke {

        @Test
        @DisplayName("deactivates the token and persists it")
        void deactivates() {
            RefreshToken token = RefreshToken.builder().active(true).build();

            service.revoke(token);

            assertThat(token.isActive()).isFalse();
            verify(refreshTokenRepository).save(token);
        }

        @Test
        @DisplayName("deletes every token owned by the profile")
        void deletesAllByProfile() {
            Profile profile = Profile.builder().build();

            service.revokeAllByProfile(profile);

            verify(refreshTokenRepository).deleteByProfile(profile);
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsValid {

        @Test
        @DisplayName("true while the expiry is in the future")
        void future() {
            RefreshToken token = RefreshToken.builder().expiryDate(Instant.now().plusSeconds(120)).build();

            assertThat(service.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("false once the expiry has passed")
        void past() {
            RefreshToken token = RefreshToken.builder().expiryDate(Instant.now().minusSeconds(120)).build();

            assertThat(service.isTokenValid(token)).isFalse();
        }
    }
}
