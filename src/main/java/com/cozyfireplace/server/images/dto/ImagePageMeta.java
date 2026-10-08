package com.cozyfireplace.server.images.dto;

public record ImagePageMeta(
        int size,
        int number,
        long totalElements,
        int totalPages
) {
}
