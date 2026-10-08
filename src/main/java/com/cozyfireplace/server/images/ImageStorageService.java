package com.cozyfireplace.server.images;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private final S3Client s3Client;
    private final ImageStorageProperties props;

    public void put(String objectKey, byte[] data, String contentType) {
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(objectKey)
                    .contentType(contentType)
                    .build(), RequestBody.fromBytes(data));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to store object " + objectKey, e);
        }
    }

    public void delete(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(objectKey)
                    .build());
        } catch (Exception e) {
            // Log but don't fail the request — the DB row removal is the source of truth
            // for the frontend's "broken image -> placeholder" behaviour.
            log.warn("Failed to delete object '{}' from storage: {}", objectKey, e.getMessage());
        }
    }

    public String publicUrl(String objectKey) {
        String base = props.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            base = props.getEndpoint() + "/" + props.getBucket();
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + objectKey;
    }
}
