package com.cozyfireplace.server.images.dto;

public record ImageQuotaResponse(
        long usedBytes,
        long limitBytes
) {
}
