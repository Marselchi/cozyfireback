package com.cozyfireplace.server.images;

/**
 * Thrown when an upload would push the account past its storage quota.
 * Mapped to HTTP 409 with body {@code {"error":"quota_exceeded"}}.
 */
public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(long usedBytes, long incomingBytes, long limitBytes) {
        super("Quota exceeded: used=%d + incoming=%d > limit=%d".formatted(usedBytes, incomingBytes, limitBytes));
    }
}
