package com.cozyfireplace.server.notifications;

import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.notifications.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RoomNotificationPublisher {

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;
    private final NotificationMapper notificationMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(RoomNotificationCommand command) {
        log.debug("Publishing notification: {} for target {}/{}",
                command.eventCode(), command.targetType(), command.targetId());

        // 1. Определяем получателей и их каналы
        Map<Long, EnumSet<NotificationChannel>> channelsByAccount = resolveRecipients(command);

        if (channelsByAccount.isEmpty()) {
            return;
        }

        // 2. Строим маршрут один раз для всех
        String route = buildRoute(command);

        // 3. Создаём сущности уведомлений
        List<Notification> notifications = channelsByAccount.keySet().stream()
                .map(accountId -> {
                    Notification n = notificationMapper.toEntity(command, route);
                    n.setAccount(accountRepository.getReferenceById(accountId));
                    return n;
                })
                .toList();

        // 4. Сохраняем
        notificationRepository.saveAll(notifications);

        // 5. Готовим элементы для диспетчеризации (вебсокеты/пуши)
        List<NotificationDispatchItem> dispatchItems = notifications.stream()
                .map(n -> new NotificationDispatchItem(
                        n.getAccount().getId(),
                        notificationMapper.toDto(n),
                        channelsByAccount.get(n.getAccount().getId())
                ))
                .toList();

        if (!dispatchItems.isEmpty()) {
            eventPublisher.publishEvent(new NotificationsCreatedEvent(dispatchItems));
        }
    }

    private String buildRoute(RoomNotificationCommand command) {
        if (command.answerId() != null && command.targetId() != null) {
            return "/" + command.roomName() + "/" + command.targetType().getPathPrefix() + "?questionId=" + command.targetId() + "&answer=" + command.answerId();
        }

        if (command.targetId() != null && command.targetType() != null) {
            return "/" + command.roomName() + "/" + command.targetType().getPathPrefix() + "/" + command.targetId();
        }

        return "/";
    }

    private Map<Long, EnumSet<NotificationChannel>> resolveRecipients(RoomNotificationCommand command) {
        return switch (command.targetType()) {
            case LORE -> {
                boolean isLoreUpdate = command.eventCode().equals(NotificationEventCode.CONTENT_UPDATED);
                yield isLoreUpdate
                        ? getRoomSubscribersForLoreUpdate(command)
                        : getRoomSubscribers(command);
            }
            case QUESTION -> {
                if (!command.eventCode().equals(NotificationEventCode.QA_NEW_QUESTION)) {
                    yield getPersonalSubscriber(command);
                }
                yield getRoomSubscribers(command);
            }
            default -> getRoomSubscribers(command);
        };
    }

    private Map<Long, EnumSet<NotificationChannel>> getPersonalSubscriber(RoomNotificationCommand command) {
        if (command.recipientId() == null) {
            return Collections.emptyMap();
        }

        var rows = preferenceRepository.findChannelsForAccount(
                command.recipientId(),
                command.eventCode()
        );

        return mapToChannels(rows);
    }

    private Map<Long, EnumSet<NotificationChannel>> getRoomSubscribers(RoomNotificationCommand command) {
        boolean restrictByRoles = command.roleIds() != null && !command.roleIds().isEmpty();

        var rows = preferenceRepository.findAccountsWithChannelsRestricted(
                command.roomId(),
                command.eventCode(),
                command.authorAccountId(),
                restrictByRoles,
                restrictByRoles ? command.roleIds() : Collections.emptySet()
        );

        return mapToChannels(rows);
    }

    private Map<Long, EnumSet<NotificationChannel>> getRoomSubscribersForLoreUpdate(RoomNotificationCommand command) {
        boolean restrictByRoles = command.roleIds() != null && !command.roleIds().isEmpty();

        var rows = preferenceRepository.findAccountsWithChannelsRestrictedLoreUpdated(
                command.roomId(),
                command.eventCode(),
                command.authorAccountId(),
                restrictByRoles,
                restrictByRoles ? command.roleIds() : Collections.emptySet(),
                command.targetId()
        );

        return mapToChannels(rows);
    }

    private Map<Long, EnumSet<NotificationChannel>> mapToChannels(List<AccountChannelView> rows) {
        Map<Long, EnumSet<NotificationChannel>> channelsByAccount = new HashMap<>();
        for (var row : rows) {
            channelsByAccount
                    .computeIfAbsent(row.getAccountId(), k -> EnumSet.noneOf(NotificationChannel.class))
                    .add(row.getChannel());
        }
        return channelsByAccount;
    }
}