package com.cozyfireplace.server.notifications.firebase;

import com.cozyfireplace.server.auth.profile.Profile;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "fcm_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    private boolean active;

    private Instant createdAt;

    private Instant lastUsedAt;
}
