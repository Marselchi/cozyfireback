package com.cozyfireplace.server.rooms;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.lore.LoreExportService;
import com.cozyfireplace.server.lore.LoreImportService;
import com.cozyfireplace.server.rooms.dto.LoreExportRequest;
import com.cozyfireplace.server.rooms.dto.LoreImportRequest;
import com.cozyfireplace.server.rooms.dto.RoomContextResponse;
import com.cozyfireplace.server.rooms.dto.RoomCreateRequest;
import com.cozyfireplace.server.rooms.dto.RoomDetailsChangeRequest;
import com.cozyfireplace.server.rooms.dto.RoomDetailsResponse;
import com.cozyfireplace.server.rooms.dto.RoomUpdateRequest;
import com.cozyfireplace.server.rooms.roomDetails.RoomDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RoomController}.
 * <p>
 * Thin facade over the room services and lore import/export services. Pins status codes and
 * argument forwarding, and covers the import filetype dispatch ({@code md}/{@code zip}/invalid)
 * and the octet-stream download headers on export.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoomController")
class RoomControllerTest {

    private static final Long ROOM_ID = 1L;

    @Mock
    private RoomService roomService;
    @Mock
    private RoomDetailsService roomDetailsService;
    @Mock
    private LoreImportService loreImportService;
    @Mock
    private LoreExportService loreExportService;

    @InjectMocks
    private RoomController controller;

    @Test
    @DisplayName("POST creates a room and returns 201")
    void createRoom() {
        RoomCreateRequest request = mock(RoomCreateRequest.class);

        ResponseEntity<Void> response = controller.createRoom(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(roomService).createRoom(request);
    }

    @Test
    @DisplayName("GET /my returns the user's rooms")
    void getUserRooms() {
        List<RoomContextResponse> rooms = List.of(mock(RoomContextResponse.class));
        when(roomService.getUserRooms()).thenReturn(rooms);

        ResponseEntity<List<RoomContextResponse>> response = controller.getUserRooms();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(rooms);
    }

    @Test
    @DisplayName("GET /{roomId}/context forwards the current account")
    void getRoomContext() {
        Account account = mock(Account.class);
        RoomContextResponse body = mock(RoomContextResponse.class);
        when(roomService.getRoomById(ROOM_ID, account)).thenReturn(body);

        ResponseEntity<RoomContextResponse> response = controller.getRoomContext(ROOM_ID, account);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
    }

    @Test
    @DisplayName("GET /{roomId}/details returns the room details")
    void getRoomDetails() {
        RoomDetailsResponse body = mock(RoomDetailsResponse.class);
        when(roomDetailsService.getRoomDetails(ROOM_ID)).thenReturn(body);

        ResponseEntity<RoomDetailsResponse> response = controller.getRoomDetails(ROOM_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
    }

    @Test
    @DisplayName("PUT /{roomId}/details returns 202 and forwards the request")
    void updateRoomDetails() {
        RoomDetailsChangeRequest request = mock(RoomDetailsChangeRequest.class);

        ResponseEntity<Void> response = controller.updateRoomDetails(ROOM_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(roomDetailsService).updateRoomDetails(request, ROOM_ID);
    }

    @Test
    @DisplayName("PUT /{roomId}/name returns 202 and forwards the rename")
    void updateName() {
        RoomUpdateRequest request = mock(RoomUpdateRequest.class);

        ResponseEntity<Void> response = controller.updateName(ROOM_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(roomService).updateRoom(ROOM_ID, request);
    }

    @Nested
    @DisplayName("POST /{roomId}/lore/import")
    class ImportLore {

        @Test
        @DisplayName("routes a md file to the markdown importer")
        void markdown() {
            MultipartFile file = mock(MultipartFile.class);
            Account account = mock(Account.class);
            LoreImportRequest filters = new LoreImportRequest("md", false, false, false);

            ResponseEntity<Void> response = controller.importLore(ROOM_ID, file, filters, account);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            verify(loreImportService).importMdFile(ROOM_ID, file, filters, account);
        }

        @Test
        @DisplayName("routes a zip file to the archive importer")
        void zip() {
            MultipartFile file = mock(MultipartFile.class);
            Account account = mock(Account.class);
            LoreImportRequest filters = new LoreImportRequest("ZIP", false, false, false);

            ResponseEntity<Void> response = controller.importLore(ROOM_ID, file, filters, account);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            verify(loreImportService).importZipFile(ROOM_ID, file, filters, account);
        }

        @Test
        @DisplayName("rejects an unsupported filetype")
        void unsupported() {
            MultipartFile file = mock(MultipartFile.class);
            Account account = mock(Account.class);
            LoreImportRequest filters = new LoreImportRequest("pdf", false, false, false);

            assertThatThrownBy(() -> controller.importLore(ROOM_ID, file, filters, account))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(loreImportService);
        }
    }

    @Test
    @DisplayName("GET /{roomId}/export returns the archive as an octet-stream download")
    void exportLore() {
        LoreExportRequest filters = mock(LoreExportRequest.class);
        Account account = mock(Account.class);
        byte[] zip = new byte[]{1, 2, 3};
        when(loreExportService.exportLore(ROOM_ID, filters, account)).thenReturn(zip);

        ResponseEntity<byte[]> response = controller.exportLore(ROOM_ID, filters, account);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("attachment; filename=\"lore-export.zip\"");
        assertThat(response.getBody()).isSameAs(zip);
    }

    @Test
    @DisplayName("GET /{roomUrl} resolves the slug to the numeric id")
    void getRoomByUrl() {
        when(roomService.getRoomByUrl("hall")).thenReturn(ROOM_ID);

        ResponseEntity<Long> response = controller.getRoomByUrl("hall");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(ROOM_ID);
    }
}
