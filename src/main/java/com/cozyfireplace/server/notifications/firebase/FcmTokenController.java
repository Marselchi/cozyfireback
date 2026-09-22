package com.cozyfireplace.server.notifications.firebase;

import com.cozyfireplace.server.notifications.firebase.dto.RegisterFcmTokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fcm")
@RequiredArgsConstructor
@Tag(name = "Push Notifications (FCM)", description = "Manage Firebase Cloud Messaging device tokens for the current user")
public class FcmTokenController {
    private final FcmTokenService tokenService;

    @Operation(
            summary = "Register a push token",
            description = "Registers (or refreshes) an FCM device token so the current user receives push notifications.",
            responses = @ApiResponse(responseCode = "200", description = "Token registered")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "FcmToken", value = """
                    { "token": "dKf9x:APA91bGh...device-token..." }
                    """))
    )
    @PostMapping("/push-token")
    public void registerToken(
            @RequestBody RegisterFcmTokenRequest request
    ) {
        tokenService.registerToken(request.token());
    }

    @Operation(
            summary = "Delete a push token",
            description = "Deactivates the given FCM device token so it no longer receives push notifications.",
            responses = @ApiResponse(responseCode = "200", description = "Token deactivated")
    )
    @DeleteMapping("/push-token")
    public void deleteToken(
            @Parameter(description = "FCM device token to deactivate", required = true)
            @RequestParam String token
    ) {
        tokenService.deactivateToken(token);
    }
}
