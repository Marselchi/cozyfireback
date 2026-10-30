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

    // Firebase Installation ID (FID). FCM is moving off registration tokens; this column
    // previously stored the device registration token and is now the FID (see CLIENT.md).
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String fid;

    private boolean active;

    private Instant createdAt;

    private Instant lastUsedAt;
}
