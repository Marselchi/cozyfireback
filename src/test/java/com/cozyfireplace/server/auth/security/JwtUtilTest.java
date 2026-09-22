package com.cozyfireplace.server.auth.security;

import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.auth.refreshToken.RefreshToken;
import com.cozyfireplace.server.auth.refreshToken.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JwtUtil}.
 * <p>
 * A real {@link JwtUtil} is constructed with a sufficiently long HMAC secret so token build/parse
 * round-trips are genuinely exercised; only the collaborating {@link RefreshTokenService} is
 * mocked. Covers access-token issuance and claim extraction, the validity/expiry checks and the
 * refresh-token format probe.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtil")
class JwtUtilTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef";

    @Mock
    private RefreshTokenService refreshTokenService;

    private JwtUtil jwtUtil;
    private Profile profile;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 3600L, 7200L, refreshTokenService);
        profile = Profile.builder().username("user").build();
    }

    @Test
    @DisplayName("an access token round-trips its subject and type claim")
    void accessClaimRoundTrip() {
        String token = jwtUtil.generateAccessToken(profile);

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("user");
        assertThat(jwtUtil.extractTokenType(token)).isEqualTo("access");
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
        assertThat(jwtUtil.isTokenValid(token, profile)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid is false for a token bound to another user")
    void wrongSubject() {
        String token = jwtUtil.generateAccessToken(profile);
        Profile other = Profile.builder().username("someone-else").build();

        assertThat(jwtUtil.isTokenValid(token, other)).isFalse();
    }

    @Nested
    @DisplayName("generateTokens")
    class GenerateTokens {

        @Test
        @DisplayName("pairs a signed access token with the created refresh token value")
        void combinesAccessAndRefresh() {
            when(refreshTokenService.createRefreshToken(any(Profile.class), anyLong()))
                    .thenReturn(RefreshToken.builder().token("refresh-value").build());

            JwtUtil.GeneratedTokens tokens = jwtUtil.generateTokens(profile);

            assertThat(tokens.refreshToken()).isEqualTo("refresh-value");
            assertThat(jwtUtil.extractUsername(tokens.accessToken())).isEqualTo("user");
        }
    }

    @Nested
    @DisplayName("isRefreshTokenFormat")
    class Format {

        @Test
        @DisplayName("accepts a UUID-formatted value")
        void uuid() {
            assertThat(jwtUtil.isRefreshTokenFormat(UUID.randomUUID().toString())).isTrue();
        }

        @Test
        @DisplayName("rejects an arbitrary string")
        void nonUuid() {
            assertThat(jwtUtil.isRefreshTokenFormat("not-a-uuid")).isFalse();
        }
    }
}
