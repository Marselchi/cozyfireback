package com.cozyfireplace.server.auth;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.auth.refreshToken.RefreshToken;
import com.cozyfireplace.server.auth.refreshToken.RefreshTokenRepository;
import com.cozyfireplace.server.invitations.Invitation;
import com.cozyfireplace.server.invitations.InvitationRepository;
import com.cozyfireplace.server.rooms.Room;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the authentication module ({@code /api/v1/auth/**}).
 *
 * <p>Covers the complete token lifecycle end-to-end against a real PostgreSQL +
 * the real Spring Security filter chain: registration (guarded by an invitation code),
 * login (JWT access token + refresh-token cookie), profile retrieval with a Bearer token,
 * silent refresh and logout (refresh token revocation).</p>
 *
 * <p>These endpoints are {@code permitAll} in {@code SecurityConfig}, so no
 * {@code SecurityContextHolder} fixtures are needed — authentication flows through
 * real credentials.</p>
 */
@DisplayName("Auth module IT: register / login / me / refresh / logout")
class AuthIT extends BaseIntegrationTest {

    private static final String VALID_CODE = "IT-COZY-2026";
    private static final String RAW_PASSWORD = "strongPass1";

    @Autowired
    private InvitationRepository invitationRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    /** Seeds an invitation with {@link #VALID_CODE} bound to a fresh room. */
    private void seedInvitationCode() {
        Room room = newRoom("auth-room-" + System.nanoTime());
        invitationRepository.save(Invitation.builder().code(VALID_CODE).room(room).build());
    }

    private String signupBody(String username, String email, String code) {
        return """
                {"username":"%s","email":"%s","password":"%s","code":"%s"}
                """.formatted(username, email, RAW_PASSWORD, code).trim();
    }

    // ====== POST /api/v1/auth/register ======
    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("should create profile with hashed password when invitation code is valid")
        void register_success() throws Exception {
            seedInvitationCode();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody("newbie", "newbie@it.cozyfireplace.ru", VALID_CODE)))
                    .andExpect(status().isCreated());

            Profile saved = profileRepository.findByUsername("newbie").orElseThrow();
            assertThat(saved.getEmail(), equalTo("newbie@it.cozyfireplace.ru"));
            // Password must be BCrypt-hashed, never stored in plain text
            assertThat(saved.getPassword(), not(equalTo(RAW_PASSWORD)));
            assertThat(saved.getPassword(), startsWith("$2"));
        }

        @Test
        @DisplayName("should return 409 when username is already taken")
        void register_duplicateUsername() throws Exception {
            seedInvitationCode();
            newProfile("dup"); // helper usernames are unique; take one for the conflict check below
            Profile taken = profileRepository.findAll().getFirst();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody(taken.getUsername(), "other@it.cozyfireplace.ru", VALID_CODE)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("Username")));
        }

        @Test
        @DisplayName("should return 400 when username is shorter than 3 chars")
        void register_validationError() throws Exception {
            seedInvitationCode();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody("ab", "ab@it.cozyfireplace.ru", VALID_CODE)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.username", notNullValue()));
        }

        @Test
        @DisplayName("should not create a profile when invitation code is unknown (current silent behaviour)")
        void register_invalidCode_isIgnored() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody("ghost", "ghost@it.cozyfireplace.ru", "NO-SUCH-CODE")))
                    .andExpect(status().isCreated()); // service returns silently -> controller still 201

            assertThat(profileRepository.findByUsername("ghost").isPresent(), is(false));
        }
    }

    // ====== POST /api/v1/auth/login ======
    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        private void registerUser(String username) throws Exception {
            seedInvitationCode();
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(signupBody(username, username + "@it.cozyfireplace.ru", VALID_CODE)));
        }

        @Test
        @DisplayName("should return access token and set refresh cookie on success")
        void login_success() throws Exception {
            registerUser("walker");

            var result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"walker","password":"strongPass1"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", not(emptyString())))
                    .andReturn();

            Cookie refresh = result.getResponse().getCookie("refreshToken");
            assertThat("refreshToken cookie must be set on login", refresh, notNullValue());
            assertThat(refresh.getValue(), not(emptyString()));

            List<RefreshToken> tokens = refreshTokenRepository.findAll();
            assertThat(tokens, hasSize(1));
            assertThat(tokens.getFirst().isActive(), is(true));
        }

        @Test
        @DisplayName("should reject login with wrong password")
        void login_wrongPassword() throws Exception {
            registerUser("mypass");

            // BadCredentialsException is not mapped explicitly; today it falls through to
            // GlobalExceptionHandler's generic handler. The contract that matters: never 2xx.
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"mypass","password":"wrongPass1"}
                                    """))
                    .andExpect(result -> assertThat("failed login must not return 2xx",
                            result.getResponse().getStatus(), greaterThanOrEqualTo(400))
                    );
        }

        @Test
        @DisplayName("should return 400 when username is blank")
        void login_blankUsername() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"","password":"whatever"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.username", notNullValue()));
        }
    }

    // ====== GET /api/v1/auth/me ======
    @Nested
    @DisplayName("GET /api/v1/auth/me")
    class MeTests {

        @Test
        @DisplayName("should return current profile when called with a valid Bearer token")
        void me_withValidBearerToken() throws Exception {
            seedInvitationCode();
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(signupBody("readme", "readme@it.cozyfireplace.ru", VALID_CODE)));

            var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"username":"readme","password":"strongPass1"}
                            """)).andReturn();
            String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                    .get("accessToken").asString();

            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username", equalTo("readme")))
                    .andExpect(jsonPath("$.email", equalTo("readme@it.cozyfireplace.ru")))
                    .andExpect(jsonPath("$.id", notNullValue()));
        }

        @Test
        @DisplayName("should fail (500) when called anonymously — documented current behaviour")
        void me_unauthenticated() throws Exception {
            clearAuthentication();

            // /api/v1/auth/** is permitAll, so the request reaches the controller without a
            // SecurityContext; AuthService.getProfile() then dereferences a null authentication.
            // This test pins the CURRENT behaviour (NPE -> generic handler -> 500) and will fail
            // once a proper 401/403 entry point or null-check is introduced.
            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ====== POST /api/v1/auth/refresh + logout ======
    @Nested
    @DisplayName("POST /api/v1/auth/refresh and /logout")
    class RefreshLogoutTests {

        private void registerAndLogin(String username) {
            try {
                seedInvitationCode();
                mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(username, username + "@it.cozyfireplace.ru", VALID_CODE)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private String loginAndGetRefreshCookie(String username) throws Exception {
            var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"username":"%s","password":"strongPass1"}
                            """.formatted(username)))
                    .andExpect(status().isOk())
                    .andReturn();
            return loginResult.getResponse().getCookie("refreshToken").getValue();
        }

        @Test
        @DisplayName("should issue a new access token and rotate the refresh token")
        void refresh_success() throws Exception {
            registerAndLogin("refresher");
            String oldRefresh = loginAndGetRefreshCookie("refresher");

            var result = mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(new Cookie("refreshToken", oldRefresh)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", not(emptyString())))
                    .andReturn();

            String newRefresh = result.getResponse().getCookie("refreshToken").getValue();
            assertThat("refresh token must be rotated", newRefresh, not(equalTo(oldRefresh)));

            // old token was invalidated, new one is active in DB
            assertThat("old refresh token must be revoked", refreshTokenRepository.findByTokenAndActiveTrue(oldRefresh).isPresent(), is(false));
            assertThat("rotated refresh token must be active", refreshTokenRepository.findByTokenAndActiveTrue(newRefresh).isPresent(), is(true));
        }

        @Test
        @DisplayName("should return 404 when refresh cookie is missing")
        void refresh_withoutCookie() throws Exception {
            mockMvc.perform(post("/api/v1/auth/refresh"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should reject an already revoked (rotated) refresh token")
        void refresh_reuseRevokedToken() throws Exception {
            registerAndLogin("replayer");
            String oldRefresh = loginAndGetRefreshCookie("replayer");

            mockMvc.perform(post("/api/v1/auth/refresh")
                    .cookie(new Cookie("refreshToken", oldRefresh))); // rotation revokes oldRefresh

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(new Cookie("refreshToken", oldRefresh)))
                    // RefreshTokenInvalidException has no dedicated @ExceptionHandler yet and
                    // falls through to the generic handler -> 500; asserted here to pin reality.
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("should revoke refresh token on logout")
        void logout_revokesToken() throws Exception {
            registerAndLogin("exiter");
            String refresh = loginAndGetRefreshCookie("exiter");

            mockMvc.perform(post("/api/v1/auth/logout")
                            .cookie(new Cookie("refreshToken", refresh)))
                    .andExpect(status().isOk());

            Optional<RefreshToken> token = refreshTokenRepository.findByTokenAndActiveTrue(refresh);
            assertThat("token must be deactivated after logout", token.isPresent(), is(false));
        }
    }
}
