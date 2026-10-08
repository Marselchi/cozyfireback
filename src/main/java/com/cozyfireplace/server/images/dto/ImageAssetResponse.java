package com.cozyfireplace.server.images.dto;

import java.time.Instant;

public record ImageAssetResponse(
        String id,
        String url,
        int width,
        int height,
        long sizeBytes,
        Instant createdAt
) {
}
