package com.cozyfireplace.server.notifications.firebase;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.notifications.firebase.dto.RegisterFcmTokenRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fcm")
@RequiredArgsConstructor
public class FcmTokenController {
    private final FcmTokenService tokenService;

    @PostMapping("/push-token")
    public void registerToken(
            @RequestBody RegisterFcmTokenRequest request
    ) {
        tokenService.registerToken(request.token());
    }

    @DeleteMapping("/push-token")
    public void deleteToken(
            @RequestParam String token
    ) {
        tokenService.deactivateToken(token);
    }
}
