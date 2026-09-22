package com.cozyfireplace.server.invitations;


import com.cozyfireplace.server.invitations.dto.InvitationRequest;
import com.cozyfireplace.server.invitations.dto.InvitationRespone;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
@Tag(name = "Invitations", description = "Create, list, use and revoke room invitation codes")
public class InvitationController {

    private final InvitationService invitationService;


    @Operation(
            summary = "Create an invitation",
            description = "Generates a new invitation code for the room that can be shared to grant access.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invitation created",
                            content = @Content(examples = @ExampleObject(name = "Invitation", value = """
                                    { "id": 12, "code": "8F3K-Q2P9" }
                                    """))),
                    @ApiResponse(responseCode = "403", description = "Not allowed to create invitations", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
            }
    )
    @PostMapping("/{roomId}")
    public ResponseEntity<InvitationRespone> createInvitation(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId) {
        InvitationRespone response = invitationService.createInvitation(roomId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Use an invitation",
            description = "Joins the room by redeeming a valid invitation code.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Invitation redeemed"),
                    @ApiResponse(responseCode = "404", description = "Invitation code not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "UseInvitation", value = """
                    { "code": "8F3K-Q2P9" }
                    """))
    )
    @PostMapping("/use")
    public ResponseEntity<Void> useInvitation(@RequestBody InvitationRequest request) {
        invitationService.useInvitation(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(
            summary = "Delete an invitation",
            description = "Revokes an existing invitation so it can no longer be used.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Invitation deleted"),
                    @ApiResponse(responseCode = "404", description = "Invitation not found", content = @Content)
            }
    )
    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> deleteInvitation(
            @Parameter(description = "ID of the invitation", required = true, example = "12") @PathVariable Long invitationId) {
        invitationService.deleteInvitation(invitationId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


    @Operation(
            summary = "List a room's invitations",
            description = "Returns all active invitations for the specified room.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of invitations returned"),
                    @ApiResponse(responseCode = "403", description = "Not allowed to list invitations", content = @Content)
            }
    )
    //todo: checks
    @GetMapping("/{roomId}/all")
    public ResponseEntity<List<InvitationRespone>> getAllAccountsRoom(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId) {
        List<InvitationRespone> response = invitationService.getAllInvitations(roomId);
        return ResponseEntity.ok(response);
    }
}
