package com.cozyfireplace.server.invitations;


import com.cozyfireplace.server.accounts.AccountService;
import com.cozyfireplace.server.invitations.dto.InvitationRequest;
import com.cozyfireplace.server.invitations.dto.InvitationRespone;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.util.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvitationService {
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
    private static final SecureRandom random = new SecureRandom();
    private final InvitationRepository invitationRepository;
    private final AccountService accountService;
    private final RoomRepository roomRepository;

    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public InvitationRespone createInvitation(Long roomId) {
        Room room = roomRepository.getReferenceById(roomId);
        Invitation invitation = Invitation.builder()
                .code(generateRandomString(8))
                .room(room)
                .build();
        Invitation saved = invitationRepository.save(invitation);
        return new InvitationRespone(saved.getId(), saved.getCode());
    }

    public void deleteInvitation(Long id) {
        invitationRepository.deleteById(id);
    }

    @Transactional
    public void useInvitation(InvitationRequest request) {
        Invitation invitation = invitationRepository.findByCode(request.code())
                .orElseThrow(() -> new NotFoundException("Invitation", "code", request.code()));
        accountService.createAccount(invitation.getRoom().getId());
    }

    public List<InvitationRespone> getAllInvitations(Long roomId) {
        List<Invitation> invitationList = invitationRepository.findAllByRoomId(roomId);

        return invitationList.stream().map((i) -> new InvitationRespone(i.getId(), i.getCode())).toList();
    }
}
