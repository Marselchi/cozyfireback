package com.cozyfireplace.server.auth;

import com.cozyfireplace.server.auth.security.JwtAuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthController}.
 * <p>
 * Thin facade over {@link AuthService}: pins the login/register/me/refresh/logout status codes,
 * the body passthrough and the request/response forwarding (register returns 201, everything
 * else 200).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    @Test
    @DisplayName("POST /login returns the token with 200")
    void login() {
        LoginRequest request = mock(LoginRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JwtAuthResponse token = mock(JwtAuthResponse.class);
        when(authService.login(request, response)).thenReturn(token);

        ResponseEntity<JwtAuthResponse> result = controller.login(request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(token);
    }

    @Test
    @DisplayName("POST /register returns 201 and forwards the request")
    void signup() {
        SignupRequest request = mock(SignupRequest.class);

        ResponseEntity<Void> result = controller.signup(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(authService).signup(request);
    }

    @Test
    @DisplayName("GET /me returns the profile with 200")
    void getCurrentProfile() {
        ProfileResponse profile = mock(ProfileResponse.class);
        when(authService.getProfile()).thenReturn(profile);

        ResponseEntity<ProfileResponse> result = controller.getCurrentProfile();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(profile);
    }

    @Test
    @DisplayName("POST /refresh returns the new token with 200")
    void refresh() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JwtAuthResponse token = mock(JwtAuthResponse.class);
        when(authService.refresh(request, response)).thenReturn(token);

        ResponseEntity<JwtAuthResponse> result = controller.refresh(request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(token);
    }

    @Test
    @DisplayName("POST /logout returns 200 and delegates")
    void logout() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        ResponseEntity<Void> result = controller.logout(request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).logout(request, response);
    }
}
