package com.cozyfireplace.server.auth;

import com.cozyfireplace.server.auth.security.JwtAuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, log in, refresh and log out; issues JWT access tokens")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Log in",
            description = "Authenticates a user with username and password and returns a JWT access token (also set as an HTTP-only cookie).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login successful",
                            content = @Content(examples = @ExampleObject(name = "JwtAuthResponse", value = """
                                    { "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJteXVzZXIifQ.signature" }
                                    """))),
                    @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "Login", value = """
                    { "username": "myuser", "password": "s3cret" }
                    """))
    )
    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        JwtAuthResponse token = authService.login(request, response);
        return ResponseEntity.ok(token);
    }

    @Operation(
            summary = "Register",
            description = "Creates a new user account. Requires a valid registration code.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Account created"),
                    @ApiResponse(responseCode = "400", description = "Validation error or invalid code", content = @Content),
                    @ApiResponse(responseCode = "409", description = "Username or email already taken", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "Signup", value = """
                    {
                      "username": "myuser",
                      "email": "myuser@example.com",
                      "password": "s3cretPass",
                      "code": "COZY-2026"
                    }
                    """))
    )
    @PostMapping("/register")
    public ResponseEntity<Void> signup(@RequestBody @Valid SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(
            summary = "Get current profile",
            description = "Returns the profile of the authenticated user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile returned",
                            content = @Content(examples = @ExampleObject(name = "Profile", value = """
                                    { "id": 7, "username": "myuser", "email": "myuser@example.com" }
                                    """))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
            }
    )
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getCurrentProfile() {
        ProfileResponse profile = authService.getProfile();
        return ResponseEntity.ok(profile);
    }


    @Operation(
            summary = "Refresh access token",
            description = "Issues a new access token using the refresh cookie sent with the request.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token refreshed",
                            content = @Content(examples = @ExampleObject(name = "JwtAuthResponse", value = """
                                    { "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJteXVzZXIifQ.newsignature" }
                                    """))),
                    @ApiResponse(responseCode = "401", description = "Missing or expired refresh token", content = @Content)
            }
    )
    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        JwtAuthResponse token = authService.refresh(request, response);
        return ResponseEntity.ok(token);
    }

    @Operation(
            summary = "Log out",
            description = "Invalidates the refresh token and clears the authentication cookies.",
            responses = @ApiResponse(responseCode = "200", description = "Logged out")
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok().build();
    }
}
