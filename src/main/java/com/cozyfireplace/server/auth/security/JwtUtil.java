package com.cozyfireplace.server.auth.security;

import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.auth.refreshToken.RefreshTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {
    public record GeneratedTokens(String accessToken, String refreshToken) {}

    private final SecretKey secretKey;
    private final long accessTokenExpirationSec;
    private final long refreshTokenExpirationSec;
    private final RefreshTokenService refreshTokenService;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.accessExpiration}") long accessTokenExpirationSec,
            @Value("${jwt.refreshExpiration}") long refreshTokenExpirationSec,
            RefreshTokenService refreshTokenService
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpirationSec = accessTokenExpirationSec;
        this.refreshTokenExpirationSec = refreshTokenExpirationSec;
        this.refreshTokenService = refreshTokenService;
    }

    public GeneratedTokens generateTokens(Profile profile) {
        String accessToken = buildToken(profile, accessTokenExpirationSec, Map.of(
                "type", "access"
        ));
        String refreshToken = refreshTokenService.createRefreshToken(profile, refreshTokenExpirationSec).getToken();
        return new GeneratedTokens(accessToken, refreshToken);
    }

    public String generateAccessToken(Profile profile) {
        return buildToken(profile, accessTokenExpirationSec, Map.of(
                "type", "access"
        ));
    }

    private String buildToken(Profile profile, long expirationSec, Map<String, Object> extraClaims) {
        return Jwts.builder()
                .header().type("JWT").and()
                .claims(extraClaims)
                .subject(profile.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationSec * 1000L)) //в секунды
                .id(UUID.randomUUID().toString())
                .signWith(secretKey)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractTokenType(String token) {
        return (String) extractAllClaims(token).get("type");
    }

    public boolean isTokenValid(String token, Profile user) {
        final String username = extractUsername(token);
        return (username.equals(user.getUsername()) && !isTokenExpired(token));
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public boolean isRefreshTokenFormat(String token) {
        try {
            UUID.fromString(token);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token) {
        return refreshTokenService.findByToken(token)
                .filter(refreshTokenService::isTokenValid)
                .isPresent();
    }

    public Profile getProfileByRefreshToken(String token) {
        return refreshTokenService.findByToken(token)
                .filter(refreshTokenService::isTokenValid)
                .map(com.cozyfireplace.server.auth.refreshToken.RefreshToken::getProfile)
                .orElse(null);
    }

    public void invalidateRefreshToken(String token) {
        refreshTokenService.findByToken(token)
                .ifPresent(refreshTokenService::revoke);
    }
}