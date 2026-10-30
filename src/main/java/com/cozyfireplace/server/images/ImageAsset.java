package com.cozyfireplace.server.images;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "image_assets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageAsset {

    /** Opaque string id, e.g. {@code img_01HXYZ...}. */
    @Id
    @Column(length = 64, nullable = false, updatable = false)
    private String id;

    /** Owner. Images belong to the profile (user), shared across every room. */
    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    /** Object key inside the bucket, kept so DELETE can remove the stored blob. */
    @Column(name = "object_key", nullable = false, columnDefinition = "TEXT")
    private String objectKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
