package com.cozyfireplace.server.images;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "images.storage")
public class ImageStorageProperties {

    /** RustFS/S3-compatible endpoint, e.g. http://localhost:9000 */
    private String endpoint = "http://localhost:9000";

    private String accessKey = "rustfsadmin";

    private String secretKey = "rustfsadmin";

    /** Single bucket for all image assets. */
    private String bucket = "user-uploads";

    /**
     * Optional public base URL used to build the {@code url} field returned to clients
     * (CDN or bucket-public endpoint). When blank, falls back to {@code endpoint/bucket}.
     */
    private String publicBaseUrl;

    /** Hard per-file cap applied AFTER server-side WebP re-encode. */
    private long maxFileBytes = 8L * 1024 * 1024;

    /** Per-account (profile) storage quota. */
    private long quotaBytes = 100L * 1024 * 1024;

    /** WebP encoder quality (0.0 - 1.0). */
    private float webpQuality = 0.85f;
}
