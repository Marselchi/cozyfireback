package com.cozyfireplace.server.notifications;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.notifications.dto.NotificationMapper;
import com.cozyfireplace.server.notifications.dto.NotificationResponse;
import com.cozyfireplace.server.util.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationInboxService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getRoomNotifications(Account account) {
        return notificationRepository
                .findAllByAccountIdOrderByCreatedAtDesc(account.getId())
                .stream()
                .map(notificationMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Account account) {
        return notificationRepository.countByAccountIdAndReadAtNull(account.getId());
    }

    public NotificationResponse markAsRead(Long notificationId, Account account) {
        Notification notification = notificationRepository
                .findByIdAndAccountId(notificationId, account.getId())
                .orElseThrow(() -> new NotFoundException("Notification", notificationId));

        notification.setReadAt(Instant.now());
        return notificationMapper.toDto(notification);
    }

    public void markAllAsRead(Account account) {
        notificationRepository.markAllAsRead(account.getId(), Instant.now());
    }

    public void delete(Long notificationId, Account account) {
        Notification notification = notificationRepository
                .findByIdAndAccountId(notificationId, account.getId())
                .orElseThrow(() -> new NotFoundException("Notification", notificationId));

        notificationRepository.delete(notification);
    }

    public void deleteAll(Account account) {
        notificationRepository.deleteAllByAccountId(account.getId());
    }
}