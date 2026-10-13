package com.cozyfireplace.server.images;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.rooms.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link ImageController} — {@code /api/v1/images}.
 *
 * <p>Exercises the real MVC + Spring Security + JPA stack against the shared Testcontainers
 * PostgreSQL. The object storage client ({@link software.amazon.awssdk.services.s3.S3Client}) and
 * Firebase are mocked by {@link BaseIntegrationTest}, so uploads never touch a real bucket — the
 * storage layer simply no-ops the put/delete while the {@link ImageAsset} DB row is the source of
 * truth the assertions read back.</p>
 *
 * <p>Images are <b>profile-scoped</b> (shared across every room the account is in); the
 * {@code {roomId}} path segment is only used for routing consistency and to resolve the acting
 * {@code @CurrentAccount} — the actual owner is the authenticated profile returned by
 * {@code RoomService.getCurrentAuthenticatedProfile()}.</p>
 *
 * <p><b>Documented status map</b> (consistent across the whole IT suite):
 * <ul>
 *   <li>anonymous request → 403;</li>
 *   <li>{@code @CurrentAccount} endpoints with a stranger / unknown room → 500
 *       (the argument resolver's AccessDeniedException is unmapped);</li>
 *   <li>every {@link ImageService} validation / not-found failure raises a
 *       {@code ResponseStatusException} (400 missing-file, 415 unsupported type, 400 undecodable,
 *       404 unknown image) — there is <b>no</b> {@code @ExceptionHandler} for it, so the generic
 *       handler collapses every one of them to <b>500</b> (pinned);</li>
 *   <li>a genuine {@link QuotaExceededException} → 409 (mapped explicitly).</li>
 * </ul>
 *
 * <p>The upload happy path (valid PNG re-encoded to WebP via the bundled {@code webp-imageio}
 * plugin) asserts 201; if the native WebP encoder were ever unavailable the service would throw
 * and the test would fail loudly rather than silently pass.</p>
 */
@DisplayName("ImageIT — /api/v1/images")
class ImageIT extends BaseIntegrationTest {

    @Autowired
    ImageAssetRepository imageAssetRepository;

    Profile creatorProfile;
    Room room;
    Account creatorAccount;
    Profile memberProfile;
    Account memberAccount;

    @BeforeEach
    void seedRoom() {
        creatorProfile = newProfile("dm");
        room = newRoomOwnedBy(creatorProfile, "Art Room");
        creatorAccount = room.getCreator();
        memberProfile = newProfile("player");
        memberAccount = newAccount(memberProfile, room, "Player One");
    }

    /** Encodes a small real PNG so the server can decode + re-encode it to WebP. */
    private static byte[] tinyPng() throws Exception {
        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private MockMultipartFile pngFile(byte[] content) {
        return new MockMultipartFile("file", "art.png", "image/png", content);
    }

    private long uploadAndReadSizeBytes(Profile actor) throws Exception {
        authenticateAs(actor);
        MvcResult result = mockMvc.perform(multipart("/api/v1/images/{roomId}", room.getId())
                        .file(pngFile(tinyPng())))
                .andExpect(status().isCreated())
                .andReturn();
        long size = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("sizeBytes").asLong();
        assertThat(size).isPositive();
        return size;
    }

    @Nested
    @DisplayName("GET /api/v1/images/{roomId} — list")
    class ListTests {

        @Test
        @DisplayName("empty library returns an empty page")
        void list_empty() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/images/{roomId}", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.page.totalElements").value(0));
        }

        @Test
        @DisplayName("reflects a freshly uploaded image (JPQL query auto-flushes the row)")
        void list_afterUpload() throws Exception {
            uploadAndReadSizeBytes(creatorProfile);

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/images/{roomId}", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.page.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].url").exists());
        }

        @Test
        @DisplayName("unknown room id → 500 (@CurrentAccount cannot resolve an account there)")
        void list_unknownRoom_returns500_currentBehaviour() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/images/{roomId}", 9_999_999L))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("anonymous → 403")
        void list_anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(get("/api/v1/images/{roomId}", room.getId()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/images/{roomId}/quota")
    class QuotaTests {

        @Test
        @DisplayName("fresh account reports zero used against the configured limit")
        void quota_empty() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/images/{roomId}/quota", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.usedBytes").value(0))
                    .andExpect(jsonPath("$.limitBytes").value(104857600));
        }

        @Test
        @DisplayName("used bytes grow after an upload")
        void quota_afterUpload() throws Exception {
            long size = uploadAndReadSizeBytes(creatorProfile);

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/images/{roomId}/quota", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.limitBytes").value(104857600))
                    .andExpect(jsonPath("$.usedBytes").value(size));
        }

        @Test
        @DisplayName("stranger without an account in the room → 500")
        void quota_nonMember_returns500_currentBehaviour() throws Exception {
            authenticateAs(newProfile("stranger"));
            mockMvc.perform(get("/api/v1/images/{roomId}/quota", room.getId()))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/images/{roomId} — upload")
    class UploadTests {

        @Test
        @DisplayName("valid PNG uploads, re-encodes to WebP and persists the asset row → 201")
        void upload_success() throws Exception {
            long size = uploadAndReadSizeBytes(creatorProfile);

            assertThat(imageAssetRepository.sumSizeBytesByProfileId(creatorProfile.getId())).isEqualTo(size);
        }

        @Test
        @DisplayName("empty file → 500 (ResponseStatusException 400 is unmapped)")
        void upload_emptyFile_returns500_currentBehaviour() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(multipart("/api/v1/images/{roomId}", room.getId())
                            .file(new MockMultipartFile("file", "empty.png", "image/png", new byte[0])))
                    // Pinned: intended 400 collapses to 500 via the generic handler.
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("unsupported content type → 500 (ResponseStatusException 415 is unmapped)")
        void upload_unsupportedType_returns500_currentBehaviour() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(multipart("/api/v1/images/{roomId}", room.getId())
                            .file(new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes())))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("declared image/png but not decodable → 500 (ResponseStatusException 400 is unmapped)")
        void upload_undecodable_returns500_currentBehaviour() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(multipart("/api/v1/images/{roomId}", room.getId())
                            .file(new MockMultipartFile("file", "fake.png", "image/png", "not-an-image".getBytes())))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("anonymous → 403")
        void upload_anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(multipart("/api/v1/images/{roomId}", room.getId())
                            .file(pngFile(tinyPng())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/images/{roomId}/{imageId}")
    class DeleteTests {

        @Test
        @DisplayName("deletes an owned image → 204 and removes the row")
        void delete_success() throws Exception {
            authenticateAs(creatorProfile);
            MvcResult result = mockMvc.perform(multipart("/api/v1/images/{roomId}", room.getId())
                            .file(pngFile(tinyPng())))
                    .andExpect(status().isCreated())
                    .andReturn();
            String imageId = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .path("id").asString();

            mockMvc.perform(delete("/api/v1/images/{roomId}/{imageId}", room.getId(), imageId))
                    .andExpect(status().isNoContent());

            assertThat(imageAssetRepository.findByIdAndProfileId(imageId, creatorProfile.getId())).isEmpty();
        }

        @Test
        @DisplayName("unknown image id → 500 (ResponseStatusException 404 is unmapped)")
        void delete_unknown_returns500_currentBehaviour() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(delete("/api/v1/images/{roomId}/{imageId}", room.getId(), "img_does_not_exist"))
                    // Pinned: intended 404 collapses to 500 via the generic handler.
                    .andExpect(status().isInternalServerError());
        }
    }
}
