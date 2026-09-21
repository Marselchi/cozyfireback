package com.cozyfireplace.server.notifications.dispatcher;

import com.cozyfireplace.server.notifications.dto.NotificationResponse;
import com.cozyfireplace.server.notifications.firebase.FcmTokenService;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PushNotificationDispatcher {

    private final FcmTokenService tokenService;

    @Async
    public void sendAsync(Long accountId, NotificationResponse notification) {

        List<String> tokens = tokenService.getActiveTokens(accountId);

        if (tokens.isEmpty()) return;

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .putData("title", notification.title())
                .putData("type", notification.type().name())
                .putData("roomUrl", notification.roomName())
                .putData("body", notification.body())
                .putData("route", notification.route())
                .setWebpushConfig(WebpushConfig.builder()
                        .setNotification(WebpushNotification.builder()
                                .setTitle(notification.title())
                                .setBody(notification.body())
                                .setTag("room-" + notification.roomId())
                                .build())
                        .build())
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            handleResponse(tokens, response);

        } catch (FirebaseMessagingException e) {
            // лог
            e.printStackTrace();
        }
    }

    private void handleResponse(List<String> tokens, BatchResponse response) {

        List<SendResponse> responses = response.getResponses();

        for (int i = 0; i < responses.size(); i++) {
            SendResponse r = responses.get(i);

            if (!r.isSuccessful()) {
                String failedToken = tokens.get(i);
                if (isInvalidToken(r.getException())) {
                    tokenService.deactivateToken(failedToken);
                }
            }
        }
    }

    private boolean isInvalidToken(FirebaseMessagingException e) {
        return e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT;
    }
}
