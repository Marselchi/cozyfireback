package com.cozyfireplace.server.notifications;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.notifications.dto.NotificationPreferenceRequest;
import com.cozyfireplace.server.notifications.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Notification inbox and per-event delivery preferences")
public class NotificationController {

    private final NotificationInboxService notificationInboxService;
    private final NotificationPreferenceService notificationPreferenceService;

    @Operation(
            summary = "List notifications",
            description = "Returns the notification inbox for the authenticated account.",
            responses = @ApiResponse(responseCode = "200", description = "List of notifications returned")
    )
    @GetMapping("/{roomId}")
    @SuppressWarnings("unused")
    public ResponseEntity<List<NotificationResponse>> getAll(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        List<NotificationResponse> notifications = notificationInboxService.getRoomNotifications(account);
        return ResponseEntity.ok(notifications);
    }

    @Operation(
            summary = "Count unread notifications",
            description = "Returns the number of unread notifications for the authenticated account.",
            responses = @ApiResponse(responseCode = "200", description = "Unread count returned",
                    content = @Content(examples = @ExampleObject(name = "count", value = "3")))
    )
    @GetMapping("/{roomId}/unread-count")
    @SuppressWarnings("unused")
    public ResponseEntity<Long> unreadCount(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        long count = notificationInboxService.unreadCount(account);
        return ResponseEntity.ok(count);
    }

    @Operation(
            summary = "Mark a notification as read",
            description = "Marks a single notification as read for the authenticated account.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Notification marked as read"),
                    @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content)
            }
    )
    @PostMapping("/{roomId}/{notificationId}/read")
    @SuppressWarnings("unused")
    public ResponseEntity<NotificationResponse> markAsRead(
            @Parameter(description = "ID of the notification", required = true, example = "500") @PathVariable Long notificationId,
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        NotificationResponse response = notificationInboxService.markAsRead(notificationId, account);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Mark all notifications as read",
            description = "Marks every unread notification for the authenticated account as read.",
            responses = @ApiResponse(responseCode = "204", description = "All notifications marked as read")
    )
    @PostMapping("/{roomId}/read-all")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> markAllAsRead(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        notificationInboxService.markAllAsRead(account);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete a notification",
            description = "Deletes a single notification from the authenticated account's inbox.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Notification deleted"),
                    @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content)
            }
    )
    @DeleteMapping("/{roomId}/{notificationId}")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the notification", required = true, example = "500") @PathVariable Long notificationId,
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        notificationInboxService.delete(notificationId, account);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete all notifications",
            description = "Clears the authenticated account's notification inbox.",
            responses = @ApiResponse(responseCode = "204", description = "All notifications deleted")
    )
    @DeleteMapping("/{roomId}/all")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> deleteAll(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        notificationInboxService.deleteAll(account);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get notification preferences",
            description = "Returns the authenticated account's delivery preferences for each notification event.",
            responses = @ApiResponse(responseCode = "200", description = "List of preferences returned")
    )
    @GetMapping("/{roomId}/preferences")
    @SuppressWarnings("unused")
    public ResponseEntity<List<NotificationPreferenceRequest>> getPreferences(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        List<NotificationPreferenceRequest> preferences = notificationPreferenceService.getAll(account);
        return ResponseEntity.ok(preferences);
    }

    @Operation(
            summary = "Replace notification preferences",
            description = "Replaces the authenticated account's full set of notification delivery preferences.",
            responses = @ApiResponse(responseCode = "204", description = "Preferences saved")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "Preferences", value = """
                    [
                      { "eventCode": "QA_ANSWERED", "channel": "PUSH", "enabled": true },
                      { "eventCode": "SESSION_CREATED", "channel": "POLLING", "enabled": false }
                    ]
                    """))
    )
    @PostMapping("/{roomId}/preferences")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> replacePreferences(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @RequestBody List<NotificationPreferenceRequest> request,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        notificationPreferenceService.replaceAll(account, request);
        return ResponseEntity.noContent().build();
    }
}
