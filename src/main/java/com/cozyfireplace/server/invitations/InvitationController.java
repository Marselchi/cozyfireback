package com.cozyfireplace.server.invitations;


import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.invitations.dto.InvitationRequest;
import com.cozyfireplace.server.invitations.dto.InvitationRespone;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;
    private final RoomSecurityService  roomSecurityService;


    @PostMapping("/{roomId}")
    public ResponseEntity<InvitationRespone> createInvitation(@PathVariable Long roomId) {
        InvitationRespone response = invitationService.createInvitation(roomId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/use")
    public ResponseEntity<Void> useInvitation(@RequestBody InvitationRequest request) {
        invitationService.useInvitation(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> deleteInvitation(@PathVariable Long invitationId) {
        invitationService.deleteInvitation(invitationId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


    //todo: checks
    @GetMapping("/{roomId}/all")
    public ResponseEntity<List<InvitationRespone>> getAllAccountsRoom(@PathVariable Long roomId) {
        List<InvitationRespone> response = invitationService.getAllInvitations(roomId);
        return ResponseEntity.ok(response);
    }
}
