package com.cozyfireplace.server.notifications.listener;

import com.cozyfireplace.server.notifications.dispatcher.PushNotificationDispatcher;
import com.cozyfireplace.server.notifications.dto.NotificationChannel;
import com.cozyfireplace.server.notifications.dto.NotificationsCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationDispatchListener {

    private final PushNotificationDispatcher pushDispatcher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationsCreatedEvent event) {

        for (var item : event.items()) {
            if (item.channels().contains(NotificationChannel.PUSH)) {
                pushDispatcher.sendAsync(item.accountId(), item.notification());
            }
        }
    }
}