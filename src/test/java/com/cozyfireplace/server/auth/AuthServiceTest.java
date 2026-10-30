package com.cozyfireplace.server.auth;

import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.auth.profile.ProfileRepository;
import com.cozyfireplace.server.auth.security.JwtAuthResponse;
import com.cozyfireplace.server.auth.security.JwtUtil;
import com.cozyfireplace.server.invitations.InvitationRepository;
import com.cozyfireplace.server.util.exception.AlreadyExistsException;
import com.cozyfireplace.server.util.exception.NotFoundException;
import com.cozyfireplace.server.util.exception.RefreshTokenInvalidException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}.
 * <p>
 * The persistence layer, {@link AuthenticationManager}, {@link PasswordEncoder} and
 * {@link JwtUtil} are mocked; servlet request/response objects are mocked so the refresh-token
 * cookie lifecycle (set on login/refresh, cleared on logout) can be asserted. Covers the login,
 * signup (invitation-code gate + uniqueness checks), token-refresh and logout flows plus the
 * security-context-backed profile lookup.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static Cookie refreshTokenCookie() {
        return new Cookie("refreshToken", "rt");
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("authenticates, mints tokens and sets the refresh cookie")
        void happy() {
            LoginRequest request = mock(LoginRequest.class);
            when(request.getUsername()).thenReturn("user");
            when(request.getPassword()).thenReturn("pw");
            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("user");
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            Profile profile = Profile.builder().username("user").build();
            when(profileRepository.findByUsername("user")).thenReturn(Optional.of(profile));
            when(jwtUtil.generateTokens(profile)).thenReturn(new JwtUtil.GeneratedTokens("acc", "ref"));
            HttpServletResponse response = mock(HttpServletResponse.class);

            JwtAuthResponse result = service.login(request, response);

            assertThat(result.getAccessToken()).isEqualTo("acc");
            verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), contains("ref"));
        }

        @Test
        @DisplayName("throws AccessDenied when authentication is not established")
        void notAuthenticated() {
            LoginRequest request = mock(LoginRequest.class);
            when(request.getUsername()).thenReturn("user");
            when(request.getPassword()).thenReturn("pw");
            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(false);
            when(authenticationManager.authenticate(any())).thenReturn(authentication);

            assertThatThrownBy(() -> service.login(request, mock(HttpServletResponse.class)))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("throws NotFound when the authenticated user has no profile")
        void profileMissing() {
            LoginRequest request = mock(LoginRequest.class);
            when(request.getUsername()).thenReturn("user");
            when(request.getPassword()).thenReturn("pw");
            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("user");
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(profileRepository.findByUsername("user")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.login(request, mock(HttpServletResponse.class)))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("projects the profile of the currently authenticated username")
        void projects() {
            SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            "user", "pw",
                            java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));
            Profile profile = Profile.builder().id(9L).username("user").email("u@example.com").build();
            when(profileRepository.findByUsername("user")).thenReturn(Optional.of(profile));

            ProfileResponse response = service.getProfile();

            assertThat(response.getId()).isEqualTo(9L);
            assertThat(response.getUsername()).isEqualTo("user");
            assertThat(response.getEmail()).isEqualTo("u@example.com");
        }

        @Test
        @DisplayName("throws IllegalStateException when no profile backs the caller")
        void profileMissing() {
            SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            "ghost", "pw",
                            java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));
            when(profileRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getProfile()).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("signup")
    class Signup {

        private SignupRequest request() {
            SignupRequest request = mock(SignupRequest.class);
            when(request.getUsername()).thenReturn("user");
            when(request.getEmail()).thenReturn("u@example.com");
            when(request.getCode()).thenReturn("CODE");
            return request;
        }

        @Test
        @DisplayName("silently ignores an invalid invitation code")
        void invalidCode() {
            SignupRequest request = request();
            when(profileRepository.findByUsername("user")).thenReturn(Optional.empty());
            when(profileRepository.findByEmail("u@example.com")).thenReturn(Optional.empty());
            when(invitationRepository.findByCode("CODE")).thenReturn(Optional.empty());

            service.signup(request);

            verify(profileRepository, never()).save(any());
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("rejects a taken username")
        void usernameTaken() {
            SignupRequest request = request();
            when(profileRepository.findByUsername("user")).thenReturn(Optional.of(Profile.builder().build()));
            when(profileRepository.findByEmail("u@example.com")).thenReturn(Optional.empty());
            when(invitationRepository.findByCode("CODE")).thenReturn(Optional.of(mock(com.cozyfireplace.server.invitations.Invitation.class)));

            assertThatThrownBy(() -> service.signup(request)).isInstanceOf(AlreadyExistsException.class);
        }

        @Test
        @DisplayName("rejects a taken email")
        void emailTaken() {
            SignupRequest request = request();
            when(profileRepository.findByUsername("user")).thenReturn(Optional.empty());
            when(profileRepository.findByEmail("u@example.com")).thenReturn(Optional.of(Profile.builder().build()));
            when(invitationRepository.findByCode("CODE")).thenReturn(Optional.of(mock(com.cozyfireplace.server.invitations.Invitation.class)));

            assertThatThrownBy(() -> service.signup(request)).isInstanceOf(AlreadyExistsException.class);
        }

        @Test
        @DisplayName("encodes the password and persists a new profile")
        void happy() {
            SignupRequest request = request();
            when(request.getPassword()).thenReturn("secret");
            when(profileRepository.findByUsername("user")).thenReturn(Optional.empty());
            when(profileRepository.findByEmail("u@example.com")).thenReturn(Optional.empty());
            when(invitationRepository.findByCode("CODE")).thenReturn(Optional.of(mock(com.cozyfireplace.server.invitations.Invitation.class)));
            when(passwordEncoder.encode("secret")).thenReturn("hash");

            service.signup(request);

            ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
            verify(profileRepository).save(captor.capture());
            assertThat(captor.getValue().getUsername()).isEqualTo("user");
            assertThat(captor.getValue().getEmail()).isEqualTo("u@example.com");
            assertThat(captor.getValue().getPassword()).isEqualTo("hash");
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("throws when no refresh cookie is present")
        void noCookie() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getCookies()).thenReturn(null);

            assertThatThrownBy(() -> service.refresh(request, mock(HttpServletResponse.class)))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("throws when the presented token is invalid")
        void invalidToken() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getCookies()).thenReturn(new Cookie[]{refreshTokenCookie()});
            when(jwtUtil.isRefreshTokenValid("rt")).thenReturn(false);

            assertThatThrownBy(() -> service.refresh(request, mock(HttpServletResponse.class)))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("throws when the token maps to no profile")
        void profileMissing() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getCookies()).thenReturn(new Cookie[]{refreshTokenCookie()});
            when(jwtUtil.isRefreshTokenValid("rt")).thenReturn(true);
            when(jwtUtil.getProfileByRefreshToken("rt")).thenReturn(null);

            assertThatThrownBy(() -> service.refresh(request, mock(HttpServletResponse.class)))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("rotates tokens, revokes the old one and sets the fresh cookie")
        void happy() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getCookies()).thenReturn(new Cookie[]{refreshTokenCookie()});
            Profile profile = Profile.builder().username("user").build();
            when(jwtUtil.isRefreshTokenValid("rt")).thenReturn(true);
            when(jwtUtil.getProfileByRefreshToken("rt")).thenReturn(profile);
            when(jwtUtil.generateTokens(profile)).thenReturn(new JwtUtil.GeneratedTokens("acc2", "ref2"));
            HttpServletResponse response = mock(HttpServletResponse.class);

            JwtAuthResponse result = service.refresh(request, response);

            assertThat(result.getAccessToken()).isEqualTo("acc2");
            verify(jwtUtil).invalidateRefreshToken("rt");
            verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), contains("ref2"));
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("revokes a presented token and clears the cookie")
        void withToken() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getCookies()).thenReturn(new Cookie[]{refreshTokenCookie()});
            HttpServletResponse response = mock(HttpServletResponse.class);

            service.logout(request, response);

            verify(jwtUtil).invalidateRefreshToken("rt");
            verify(response).addCookie(any(Cookie.class));
        }

        @Test
        @DisplayName("clears the cookie even without a token to revoke")
        void withoutToken() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getCookies()).thenReturn(new Cookie[]{});
            HttpServletResponse response = mock(HttpServletResponse.class);

            service.logout(request, response);

            verify(jwtUtil, never()).invalidateRefreshToken(anyString());
            verify(response).addCookie(any(Cookie.class));
        }
    }
}
