package com.cozyfireplace.server.notifications.firebase;

import com.cozyfireplace.server.notifications.firebase.dto.RegisterFcmFidRequest;
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
@Tag(name = "Push Notifications (FCM)", description = "Manage Firebase Installation IDs (FIDs) for the current user")
public class FcmTokenController {
    private final FcmTokenService tokenService;

    @Operation(
            summary = "Register a push identifier",
            description = "Registers (or refreshes) a Firebase Installation ID so the current user receives push notifications.",
            responses = @ApiResponse(responseCode = "200", description = "FID registered")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "FcmFid", value = """
                    { "fid": "fGh1jK2LmNo...firebase-installation-id..." }
                    """))
    )
    @PostMapping("/push-token")
    public void registerFid(
            @RequestBody RegisterFcmFidRequest request
    ) {
        tokenService.registerFid(request.fid());
    }

    @Operation(
            summary = "Delete a push identifier",
            description = "Deactivates the given Firebase Installation ID so it no longer receives push notifications.",
            responses = @ApiResponse(responseCode = "200", description = "FID deactivated")
    )
    @DeleteMapping("/push-token")
    public void deleteFid(
            @Parameter(description = "Firebase Installation ID to deactivate", required = true)
            @RequestParam String fid
    ) {
        tokenService.deactivateFid(fid);
    }
}
