package com.cozyfireplace.server.images;

import com.cozyfireplace.server.images.dto.ImageAssetResponse;
import com.cozyfireplace.server.images.dto.ImagePageMeta;
import com.cozyfireplace.server.images.dto.ImagePageResponse;
import com.cozyfireplace.server.images.dto.ImageQuotaResponse;
import com.luciad.imageio.webp.CompressionType;
import com.luciad.imageio.webp.WebPWriteParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/webp", "image/png", "image/jpeg", "image/jpg", "image/gif");

    private final ImageAssetRepository imageAssetRepository;
    private final ImageStorageService imageStorageService;
    private final ImageStorageProperties props;

    @Transactional
    public ImageAssetResponse upload(MultipartFile file, Long profileId) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing 'file' field");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported content type: " + contentType);
        }

        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file");
        }

        // Re-derive real dimensions and re-encode to WebP server-side (strips EXIF,
        // never trusts the client's advisory width/height).
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(original));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not decode image");
        }
        if (image == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is not a valid image");
        }

        byte[] webp = encodeWebp(image);

        if (webp.length > props.getMaxFileBytes()) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                    "Encoded image exceeds max size of %d bytes".formatted(props.getMaxFileBytes()));
        }

        long usedBytes = imageAssetRepository.sumSizeBytesByProfileId(profileId);
        if (usedBytes + webp.length > props.getQuotaBytes()) {
            throw new QuotaExceededException(usedBytes, webp.length, props.getQuotaBytes());
        }

        String imageId = "img_" + UUID.randomUUID().toString().replace("-", "");
        String objectKey = "images/%d/%s.webp".formatted(profileId, imageId);

        imageStorageService.put(objectKey, webp, "image/webp");

        ImageAsset asset = ImageAsset.builder()
                .id(imageId)
                .profileId(profileId)
                .url(imageStorageService.publicUrl(objectKey))
                .width(image.getWidth())
                .height(image.getHeight())
                .sizeBytes((long) webp.length)
                .objectKey(objectKey)
                .createdAt(Instant.now())
                .build();

        return toResponse(imageAssetRepository.save(asset));
    }

    @Transactional(readOnly = true)
    public ImagePageResponse list(Long profileId, Pageable pageable) {
        Page<ImageAsset> page = imageAssetRepository.findByProfileIdOrderByCreatedAtDesc(profileId, pageable);
        List<ImageAssetResponse> content = page.getContent().stream().map(this::toResponse).toList();
        ImagePageMeta meta = new ImagePageMeta(
                page.getSize(),
                page.getNumber(),
                page.getTotalElements(),
                page.getTotalPages());
        return new ImagePageResponse(content, meta);
    }

    @Transactional(readOnly = true)
    public ImageQuotaResponse quota(Long profileId) {
        long used = imageAssetRepository.sumSizeBytesByProfileId(profileId);
        return new ImageQuotaResponse(used, props.getQuotaBytes());
    }

    @Transactional
    public void delete(String imageId, Long profileId) {
        ImageAsset asset = imageAssetRepository.findByIdAndProfileId(imageId, profileId)
                // 404 regardless of ownership to avoid leaking existence of other users' assets
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));

        imageStorageService.delete(asset.getObjectKey());
        imageAssetRepository.delete(asset);
    }

    private byte[] encodeWebp(BufferedImage image) {
        // WebP has no alpha channel support for TYPE_INT_ARGB in some encoders — normalise.
        BufferedImage rgb = image;
        if (image.getType() != BufferedImage.TYPE_INT_RGB && image.getColorModel().hasAlpha()) {
            rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgb.createGraphics().drawImage(image, 0, 0, null);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
        if (!writers.hasNext()) {
            // No WebP encoder registered on the classpath (e.g. the webp-imageio
            // ImageIO plugin is missing) — fail clearly instead of a raw NoSuchElementException.
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No WebP encoder available on the server");
        }
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            // The WebP writer's default param has no compression type set; ImageWriteParam
            // .setCompressionQuality throws "No compression type set!" unless a type is
            // chosen first, so select lossy explicitly before applying the quality.
            if (param instanceof WebPWriteParam webpParam) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                webpParam.setCompressionType(CompressionType.Lossy);
                param.setCompressionQuality(props.getWebpQuality());
            }
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(rgb, null, null), param);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "WebP encoding failed");
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    private ImageAssetResponse toResponse(ImageAsset a) {
        return new ImageAssetResponse(
                a.getId(), a.getUrl(), a.getWidth(), a.getHeight(), a.getSizeBytes(), a.getCreatedAt());
    }
}
