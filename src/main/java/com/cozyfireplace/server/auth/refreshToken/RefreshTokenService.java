package com.cozyfireplace.server.auth.refreshToken;

import com.cozyfireplace.server.auth.profile.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken createRefreshToken(Profile profile, long expirationSec) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .profile(profile)
                .expiryDate(Instant.now().plusSeconds(expirationSec))
                .active(true)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByTokenAndActiveTrue(token);
    }

    public void revoke(RefreshToken token) {
        token.setActive(false);
        refreshTokenRepository.save(token);
    }

    public void revokeAllByProfile(Profile profile) {
        refreshTokenRepository.deleteByProfile(profile);
    }

    public boolean isTokenValid(RefreshToken token) {
        return !token.getExpiryDate().isBefore(Instant.now());
    }
}