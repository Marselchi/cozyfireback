package com.cozyfireplace.server.images.dto;

import java.util.List;

public record ImagePageResponse(
        List<ImageAssetResponse> content,
        ImagePageMeta page
) {
}
