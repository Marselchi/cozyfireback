package com.cozyfireplace.server.auth.refreshToken;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link RefreshTokenCleanupScheduler}.
 * <p>
 * The scheduled job simply triggers the bulk delete; this pins that delegation against the mocked
 * repository.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenCleanupScheduler")
class RefreshTokenCleanupSchedulerTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenCleanupScheduler scheduler;

    @Test
    @DisplayName("cleanupExpiredTokens delegates to the bulk delete query")
    void triggersCleanup() {
        scheduler.cleanupExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredAndInactiveTokens();
    }
}
