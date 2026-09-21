package com.cozyfireplace.server.rooms;


import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.auth.AuthService;
import com.cozyfireplace.server.auth.ProfileResponse;
import com.cozyfireplace.server.lore.LoreExportService;
import com.cozyfireplace.server.lore.LoreImportService;
import com.cozyfireplace.server.rooms.dto.*;
import com.cozyfireplace.server.rooms.roomDetails.RoomDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RoomDetailsService roomDetailsService;
    private final LoreImportService loreImportService;
    private final LoreExportService loreExportService;

    @PostMapping
    public ResponseEntity<Void> createRoom(@RequestBody @Valid RoomCreateRequest request) {
        roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/my")
    public ResponseEntity<List<RoomContextResponse>> getUserRooms() {
        List<RoomContextResponse> rooms = roomService.getUserRooms();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{roomId}/context")
    public ResponseEntity<RoomContextResponse> getRoomContext(@PathVariable Long roomId, @CurrentAccount Account currentAccount) {
        RoomContextResponse room = roomService.getRoomById(roomId, currentAccount);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/{roomId}/details")
    public ResponseEntity<RoomDetailsResponse> getRoomDetails(@PathVariable Long roomId) {
        RoomDetailsResponse details = roomDetailsService.getRoomDetails(roomId);
        return ResponseEntity.ok(details);
    }

    @PutMapping("/{roomId}/details")
    public ResponseEntity<Void> updateRoomDetails(@PathVariable Long roomId, @RequestBody @Valid RoomDetailsChangeRequest request) {
        roomDetailsService.updateRoomDetails(request, roomId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
    @PutMapping("/{roomId}/name")
    public ResponseEntity<Void> updateName(@PathVariable Long roomId, @RequestBody RoomUpdateRequest request) {
        roomService.updateRoom(roomId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/{roomId}/lore/import")
    public ResponseEntity<Void> importLore(
            @PathVariable Long roomId,
            @RequestParam("file") MultipartFile file,
            @ModelAttribute LoreImportRequest filters,
            @CurrentAccount Account account
    ) {
        String filetype = filters.filetype();
        if ("md".equalsIgnoreCase(filetype)) {
            loreImportService.importMdFile(roomId, file, filters, account);
        } else if ("zip".equalsIgnoreCase(filetype)) {
            loreImportService.importZipFile(roomId, file, filters, account);
        } else {
            throw new IllegalArgumentException("Unsupported filetype: " + filetype + ". Use 'md' or 'zip'");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/{roomId}/export")
    public ResponseEntity<byte[]> exportLore(
            @PathVariable Long roomId,
            @ModelAttribute LoreExportRequest filters,
            @CurrentAccount Account account
    ) {
        byte[] zipContent = loreExportService.exportLore(roomId, filters, account);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"lore-export.zip\"")
                .body(zipContent);
    }

    @GetMapping("/{roomUrl}")
    public ResponseEntity<Long> getRoomByUrl(@PathVariable String roomUrl) {
        Long roomId = roomService.getRoomByUrl(roomUrl);
        return ResponseEntity.ok(roomId);
    }
}
