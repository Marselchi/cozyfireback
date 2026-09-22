package com.cozyfireplace.server.notifications.firebase;

import com.cozyfireplace.server.notifications.firebase.dto.RegisterFcmTokenRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link FcmTokenController}.
 * <p>
 * Thin facade over {@link FcmTokenService}: registration unwraps the token from the request body
 * and deletion forwards the raw query parameter; both return void (200 by Spring default).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FcmTokenController")
class FcmTokenControllerTest {

    private static final String TOKEN = "device-token";

    @Mock
    private FcmTokenService tokenService;

    @InjectMocks
    private FcmTokenController controller;

    @Test
    @DisplayName("POST push-token registers the unwrapped token value")
    void registerToken() {
        RegisterFcmTokenRequest request = new RegisterFcmTokenRequest(TOKEN);

        controller.registerToken(request);

        verify(tokenService).registerToken(TOKEN);
    }

    @Test
    @DisplayName("DELETE push-token deactivates the requested token")
    void deleteToken() {
        controller.deleteToken(TOKEN);

        verify(tokenService).deactivateToken(TOKEN);
    }
}
