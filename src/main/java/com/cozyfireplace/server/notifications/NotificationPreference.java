package com.cozyfireplace.server.notifications;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.notifications.dto.NotificationChannel;
import com.cozyfireplace.server.notifications.dto.NotificationEventCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "notification_preferences",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"account_id", "event_code", "channel"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationEventCode eventCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(nullable = false)
    private boolean enabled;
}