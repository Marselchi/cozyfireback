package com.cozyfireplace.server.auth;

import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.auth.profile.ProfileRepository;
import com.cozyfireplace.server.auth.security.JwtAuthResponse;
import com.cozyfireplace.server.auth.security.JwtUtil;
import com.cozyfireplace.server.invitations.Invitation;
import com.cozyfireplace.server.invitations.InvitationRepository;
import com.cozyfireplace.server.util.exception.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final ProfileRepository profileRepository;
    //DELETE WHEN RELEASED
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    private final String refreshTokenCookieName = "refreshToken";

    @Value("${jwt.refreshExpiration:604800}") // по умолчанию 7 дней
    private int refreshTokenCookieMaxAgeSeconds;
    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Transactional //non readonly because insert into refresh tokens
    public JwtAuthResponse login(LoginRequest request, HttpServletResponse response) {
        log.debug("Processing login request for user: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        if (!authentication.isAuthenticated()) {
            log.error("Authentication failed for user: {}", request.getUsername());
            throw new AccessDeniedException("Authentication failed");
        }

        String username = authentication.getName();

        Profile profile = profileRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found in DB after authentication: " + username));

        JwtUtil.GeneratedTokens tokens = jwtUtil.generateTokens(profile);
        setRefreshTokenCookie(response, tokens.refreshToken());

        log.info("User logged in successfully: {}", username);
        return new JwtAuthResponse(tokens.accessToken());
    }

    public ProfileResponse getProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Profile profile = profileRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Profile not found"));
        return ProfileResponse.from(profile);
    }

    /**
     * Регистрирует нового пользователя.
     */
    @Transactional
    public void signup(SignupRequest request) {
        log.debug("Processing signup request for user: {} and email: {}", request.getUsername(), request.getEmail());

        Optional<Profile> existingByUsername = profileRepository.findByUsername(request.getUsername());
        Optional<Profile> existingByEmail = profileRepository.findByEmail(request.getEmail());

        Optional<Invitation> existByCode = invitationRepository.findByCode(request.getCode());

        if (existByCode.isEmpty()) {
            log.debug("Processing signup request for user: {} and email: {} failed. Code {} is not valid",
                    request.getUsername(),
                    request.getEmail(),
                    request.getCode());
            return;
        }

        if (existingByUsername.isPresent()) {
            log.warn("Signup attempt with already existing username: {}", request.getUsername());
            throw new AlreadyExistsException("Username is already taken");
        }
        if (existingByEmail.isPresent()) {
            log.warn("Signup attempt with already existing email: {}", request.getEmail());
            throw new AlreadyExistsException("Email is already taken");
        }

        Profile profile = Profile.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // Хешируем пароль
                .build();

        profileRepository.save(profile);
        log.info("User signed up successfully: {}", request.getUsername());
    }

    @Transactional
    public JwtAuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);

        if (refreshToken == null || refreshToken.isEmpty()) {
            log.info("Refresh token not found in cookie");
            throw new NotFoundException("Refresh token not found");
        }

        if (!jwtUtil.isRefreshTokenValid(refreshToken)) {
            log.warn("Invalid or revoked refresh token: {}", refreshToken);
            throw new RefreshTokenInvalidException("Invalid refresh token");
        }

        Profile profile = jwtUtil.getProfileByRefreshToken(refreshToken);

        if (profile == null) {
            log.error("User associated with refresh token not found: {}", refreshToken);
            throw new NotFoundException("User associated with refresh token not found");
        }

        JwtUtil.GeneratedTokens newTokens = jwtUtil.generateTokens(profile);

        jwtUtil.invalidateRefreshToken(refreshToken);

        setRefreshTokenCookie(response, newTokens.refreshToken());

        log.info("Tokens refreshed successfully for user: {}", profile.getUsername());
        return new JwtAuthResponse(newTokens.accessToken());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        log.debug("Processing logout request");

        String refreshToken = extractRefreshTokenFromCookie(request);

        if (refreshToken != null && !refreshToken.isEmpty()) {
            jwtUtil.invalidateRefreshToken(refreshToken);
            log.debug("Refresh token invalidated: {}", refreshToken);
        } else {
            log.debug("No refresh token found in cookie for logout.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            log.info("Logging out user: {}", authentication.getName());
        }
        SecurityContextHolder.clearContext();

        deleteRefreshTokenCookie(response);

        log.info("User logged out successfully");
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (refreshTokenCookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(refreshTokenCookieName, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/refresh")
                .maxAge(refreshTokenCookieMaxAgeSeconds)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(refreshTokenCookieName, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}