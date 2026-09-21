package com.cozyfireplace.server.notifications.listener;

import com.cozyfireplace.server.notifications.RoomNotificationPublisher;
import com.cozyfireplace.server.notifications.dto.NotificationEventCode;
import com.cozyfireplace.server.notifications.dto.NotificationType;
import com.cozyfireplace.server.notifications.dto.RoomNotificationCommand;
import com.cozyfireplace.server.notifications.dto.TargetType;
import com.cozyfireplace.server.notifications.event.LoreEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoreNotificationListener {

    private final RoomNotificationPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(LoreEvent event) {
        // Формируем команду в зависимости от типа операции
        RoomNotificationCommand command = switch (event.type()) {
            case CREATE -> buildCreateCommand(event);
            case UPDATE -> buildUpdateCommand(event);
            default -> null;
        };

        if (command != null) {
            publisher.publish(command);
        }
    }

    private RoomNotificationCommand buildCreateCommand(LoreEvent event) {
        NotificationEventCode code = event.byDm()
                ? NotificationEventCode.CONTENT_CREATED_DM
                : NotificationEventCode.CONTENT_CREATED;

        return RoomNotificationCommand.builder()
                .type(NotificationType.CONTENT)
                .eventCode(code)
                .title("Новый лор")
                .body(String.format("Создан новый лор в %s: %s", event.roomName(), event.title()))
                .targetId(event.loreId())
                .targetType(TargetType.LORE)
                .roomId(event.roomId())
                .roomUrl(event.roomUrl())
                .authorAccountId(event.authorAccountId())
                .build();
    }

    private RoomNotificationCommand buildUpdateCommand(LoreEvent event) {
        return RoomNotificationCommand.builder()
                .type(NotificationType.CONTENT)
                .eventCode(NotificationEventCode.CONTENT_UPDATED)
                .title("Лор обновлён")
                .body(String.format("Обновлён лор в %s: %s", event.roomName(), event.title()))
                .targetId(event.loreId())
                .targetType(TargetType.LORE)
                .roomId(event.roomId())
                .roomUrl(event.roomUrl())
                .authorAccountId(event.authorAccountId())
                .build();
    }
}
