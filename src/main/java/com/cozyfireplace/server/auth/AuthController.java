package com.cozyfireplace.server.auth;

import com.cozyfireplace.server.auth.security.JwtAuthResponse;
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
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        JwtAuthResponse token = authService.login(request, response);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/register")
    public ResponseEntity<Void> signup(@RequestBody @Valid SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getCurrentProfile() {
        ProfileResponse profile = authService.getProfile();
        return ResponseEntity.ok(profile);
    }


    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        JwtAuthResponse token = authService.refresh(request, response);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok().build();
    }
}