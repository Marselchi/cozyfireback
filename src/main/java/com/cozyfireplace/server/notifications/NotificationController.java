package com.cozyfireplace.server.notifications;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.notifications.dto.NotificationPreferenceRequest;
import com.cozyfireplace.server.notifications.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationInboxService notificationInboxService;
    private final NotificationPreferenceService notificationPreferenceService;

    @GetMapping("/{roomId}")
    public ResponseEntity<List<NotificationResponse>> getAll(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        List<NotificationResponse> notifications = notificationInboxService.getRoomNotifications(account);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{roomId}/unread-count")
    public ResponseEntity<Long> unreadCount(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        long count = notificationInboxService.unreadCount(account);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/{roomId}/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @Parameter(description = "ID уведомления", required = true) @PathVariable Long notificationId,
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        NotificationResponse response = notificationInboxService.markAsRead(notificationId, account);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        notificationInboxService.markAllAsRead(account);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{roomId}/{notificationId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID уведомления", required = true) @PathVariable Long notificationId,
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        notificationInboxService.delete(notificationId, account);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{roomId}/all")
    public ResponseEntity<Void> deleteAll(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        notificationInboxService.deleteAll(account);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}/preferences")
    public ResponseEntity<List<NotificationPreferenceRequest>> getPreferences(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        List<NotificationPreferenceRequest> preferences = notificationPreferenceService.getAll(account);
        return ResponseEntity.ok(preferences);
    }

    @PostMapping("/{roomId}/preferences")
    public ResponseEntity<Void> replacePreferences(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @RequestBody List<NotificationPreferenceRequest> request,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        notificationPreferenceService.replaceAll(account, request);
        return ResponseEntity.noContent().build();
    }
}