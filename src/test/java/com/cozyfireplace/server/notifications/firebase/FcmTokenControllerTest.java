package com.cozyfireplace.server.notifications.firebase;

import com.cozyfireplace.server.notifications.firebase.dto.RegisterFcmFidRequest;
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

    private static final String FID = "firebase-installation-id";

    @Mock
    private FcmTokenService tokenService;

    @InjectMocks
    private FcmTokenController controller;

    @Test
    @DisplayName("POST push-token registers the unwrapped fid value")
    void registerFid() {
        RegisterFcmFidRequest request = new RegisterFcmFidRequest(FID);

        controller.registerFid(request);

        verify(tokenService).registerFid(FID);
    }

    @Test
    @DisplayName("DELETE push-token deactivates the requested fid")
    void deleteFid() {
        controller.deleteFid(FID);

        verify(tokenService).deactivateFid(FID);
    }
}
