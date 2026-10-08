package com.cozyfireplace.server.images;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.images.dto.ImageAssetResponse;
import com.cozyfireplace.server.images.dto.ImagePageResponse;
import com.cozyfireplace.server.images.dto.ImageQuotaResponse;
import com.cozyfireplace.server.rooms.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Tag(name = "Images", description = "Per-account image library: upload, list, quota and delete. " +
        "Endpoints are room-scoped for routing consistency, but images belong to the account " +
        "(profile) and are shared across every room the user is in.")
public class ImageController {

    private final ImageService imageService;
    private final RoomService roomService;

    @Operation(summary = "Upload an image",
            description = "Uploads a new image for the current account. The file is re-encoded to WebP " +
                    "server-side; advisory width/height fields are ignored. Enforces per-file max size " +
                    "and account quota.")
    @PostMapping(value = "/{roomId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageAssetResponse> upload(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId,
            @Parameter(description = "Image file (webp/png/jpeg/gif)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Advisory width (ignored, re-derived server-side)")
            @RequestParam(value = "width", required = false) Integer width,
            @Parameter(description = "Advisory height (ignored, re-derived server-side)")
            @RequestParam(value = "height", required = false) Integer height,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        Long profileId = roomService.getCurrentAuthenticatedProfile().getId();
        ImageAssetResponse response = imageService.upload(file, profileId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List account images",
            description = "Paginated list of the current account's images, newest first.")
    @GetMapping("/{roomId}")
    public ResponseEntity<ImagePageResponse> list(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId,
            @ParameterObject @PageableDefault(size = 24) Pageable pageable,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        Long profileId = roomService.getCurrentAuthenticatedProfile().getId();
        return ResponseEntity.ok(imageService.list(profileId, pageable));
    }

    @Operation(summary = "Get account image quota",
            description = "Returns used and limit bytes for the current account's image library.")
    @GetMapping("/{roomId}/quota")
    public ResponseEntity<ImageQuotaResponse> quota(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        Long profileId = roomService.getCurrentAuthenticatedProfile().getId();
        return ResponseEntity.ok(imageService.quota(profileId));
    }

    @Operation(summary = "Delete an image",
            description = "Deletes an image owned by the current account, removing both the stored " +
                    "object and the DB row. Returns 404 if the image does not exist or belongs to " +
                    "another account.")
    @DeleteMapping("/{roomId}/{imageId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId,
            @Parameter(description = "Image id", required = true, example = "img_01HXYZ")
            @PathVariable String imageId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        Long profileId = roomService.getCurrentAuthenticatedProfile().getId();
        imageService.delete(imageId, profileId);
        return ResponseEntity.noContent().build();
    }
}
