package com.cozyfireplace.server.rooms;


import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.lore.LoreExportService;
import com.cozyfireplace.server.lore.LoreImportService;
import com.cozyfireplace.server.rooms.dto.*;
import com.cozyfireplace.server.rooms.roomDetails.RoomDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Rooms", description = "Room lifecycle: create, list, view details and import/export lore")
public class RoomController {

    private final RoomService roomService;
    private final RoomDetailsService roomDetailsService;
    private final LoreImportService loreImportService;
    private final LoreExportService loreExportService;

    @Operation(
            summary = "Create a room",
            description = "Creates a new room owned by the authenticated user.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Room created"),
                    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "RoomCreate", value = """
                    { "name": "Emberhold", "description": "A cozy DnD campaign", "url": "emberhold" }
                    """))
    )
    @PostMapping
    public ResponseEntity<Void> createRoom(@RequestBody @Valid RoomCreateRequest request) {
        roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(
            summary = "List my rooms",
            description = "Returns all rooms the authenticated user is a member of.",
            responses = @ApiResponse(responseCode = "200", description = "List of rooms returned")
    )
    @GetMapping("/my")
    public ResponseEntity<List<RoomContextResponse>> getUserRooms() {
        List<RoomContextResponse> rooms = roomService.getUserRooms();
        return ResponseEntity.ok(rooms);
    }

    @Operation(
            summary = "Get room context",
            description = "Returns the room's context (identity, members, current state) for the requesting account.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Room context returned"),
                    @ApiResponse(responseCode = "403", description = "Not a member of this room", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
            }
    )
    @GetMapping("/{roomId}/context")
    public ResponseEntity<RoomContextResponse> getRoomContext(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account currentAccount) {
        RoomContextResponse room = roomService.getRoomById(roomId, currentAccount);
        return ResponseEntity.ok(room);
    }

    @Operation(
            summary = "Get room details",
            description = "Returns the room's extended details (situation, in-world date, last session).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Room details returned"),
                    @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
            }
    )
    @GetMapping("/{roomId}/details")
    public ResponseEntity<RoomDetailsResponse> getRoomDetails(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId) {
        RoomDetailsResponse details = roomDetailsService.getRoomDetails(roomId);
        return ResponseEntity.ok(details);
    }

    @Operation(
            summary = "Update room details",
            description = "Updates the room's extended details.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Room details accepted for update"),
                    @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "RoomDetailsChange", value = """
                    { "situation": "Siege of the gate", "date": "14th of Ember", "lastSession": "The party retreated to camp" }
                    """))
    )
    @PutMapping("/{roomId}/details")
    public ResponseEntity<Void> updateRoomDetails(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @RequestBody @Valid RoomDetailsChangeRequest request) {
        roomDetailsService.updateRoomDetails(request, roomId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(
            summary = "Update room name and description",
            description = "Updates the room's name and/or description.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Room accepted for update"),
                    @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "RoomUpdate", value = """
                    { "name": "Emberhold Renewed", "description": "Now with extra lore" }
                    """))
    )
    @PutMapping("/{roomId}/name")
    public ResponseEntity<Void> updateName(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @RequestBody RoomUpdateRequest request) {
        roomService.updateRoom(roomId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(
            summary = "Import lore from a file",
            description = "Uploads a Markdown ('md') or 'zip' archive and imports its contents as lore into the room. Behaviour is controlled by the accompanying filter fields.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Import accepted"),
                    @ApiResponse(responseCode = "400", description = "Unsupported filetype", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    examples = @ExampleObject(name = "LoreImport", value = """
                            file: <binary>
                            filetype: md
                            autolink: true
                            roomReset: false
                            replaceOnConflict: true
                            """))
    )
    @PostMapping("/{roomId}/lore/import")
    public ResponseEntity<Void> importLore(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "Lore file to import (md or zip)", required = true) @RequestParam("file") MultipartFile file,
            @ModelAttribute LoreImportRequest filters,
            @Parameter(hidden = true) @CurrentAccount Account account
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

    @Operation(
            summary = "Export lore to a ZIP archive",
            description = "Exports the room's lore as a downloadable ZIP archive. Options control whether tags, DM-only entries and spoilers are included.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "ZIP archive of exported lore",
                            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
                    @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
            }
    )
    @GetMapping("/{roomId}/export")
    public ResponseEntity<byte[]> exportLore(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @ModelAttribute LoreExportRequest filters,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        byte[] zipContent = loreExportService.exportLore(roomId, filters, account);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"lore-export.zip\"")
                .body(zipContent);
    }

    @Operation(
            summary = "Resolve a room by URL slug",
            description = "Returns the numeric room ID for the given room URL slug.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Room ID returned",
                            content = @Content(examples = @ExampleObject(name = "roomId", value = "1"))),
                    @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
            }
    )
    @GetMapping("/{roomUrl}")
    public ResponseEntity<Long> getRoomByUrl(
            @Parameter(description = "Room URL slug", required = true, example = "emberhold") @PathVariable String roomUrl) {
        Long roomId = roomService.getRoomByUrl(roomUrl);
        return ResponseEntity.ok(roomId);
    }
}
