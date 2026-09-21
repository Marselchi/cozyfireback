package com.cozyfireplace.server.notifications.listener;

import com.cozyfireplace.server.notifications.RoomNotificationPublisher;
import com.cozyfireplace.server.notifications.dto.NotificationEventCode;
import com.cozyfireplace.server.notifications.dto.NotificationType;
import com.cozyfireplace.server.notifications.dto.RoomNotificationCommand;
import com.cozyfireplace.server.notifications.dto.TargetType;
import com.cozyfireplace.server.notifications.event.CharacterEvent;
import com.cozyfireplace.server.notifications.event.LoreEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CharacterNotificationListener {
    private final RoomNotificationPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CharacterEvent event) {
        RoomNotificationCommand command = switch (event.type()) {
            case CREATE -> buildCreateCommand(event);
            case UPDATE -> buildUpdateCommand(event);
            default -> null;
        };

        if (command != null) {
            publisher.publish(command);
        }
    }

    private RoomNotificationCommand buildCreateCommand(CharacterEvent event) {
        NotificationEventCode code = event.byDm()
                ? NotificationEventCode.CONTENT_CREATED_DM
                : NotificationEventCode.CONTENT_CREATED;

        return RoomNotificationCommand.builder()
                .type(NotificationType.CONTENT)
                .eventCode(code)
                .title("Новый персонаж")
                .body(String.format("Создан новый персонаж в %s: %s", event.roomName(), event.name()))
                .targetId(event.characterId())
                .targetType(TargetType.CHARACTER)
                .roomId(event.roomId())
                .roomUrl(event.roomUrl())
                .authorAccountId(event.authorAccountId())
                .build();
    }

    private RoomNotificationCommand buildUpdateCommand(CharacterEvent event) {
        return RoomNotificationCommand.builder()
                .type(NotificationType.CONTENT)
                .eventCode(NotificationEventCode.CONTENT_UPDATED)
                .title("Персонаж обновлён")
                .body(String.format("Обновлён персонаж в %s: %s", event.roomName(), event.name()))
                .targetId(event.characterId())
                .targetType(TargetType.CHARACTER)
                .roomId(event.roomId())
                .roomUrl(event.roomUrl())
                .authorAccountId(event.authorAccountId())
                .build();
    }
}
