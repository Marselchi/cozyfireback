package com.cozyfireplace.server.rooms;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.auth.profile.ProfileRepository;
import com.cozyfireplace.server.questions.QuestionRepository;
import com.cozyfireplace.server.rooms.dto.RoomContextResponse;
import com.cozyfireplace.server.rooms.dto.RoomCreateRequest;
import com.cozyfireplace.server.rooms.dto.RoomMapper;
import com.cozyfireplace.server.rooms.dto.RoomUpdateRequest;
import com.cozyfireplace.server.rooms.roomDetails.RoomDetails;
import com.cozyfireplace.server.rooms.roomDetails.RoomDetailsRepository;
import com.cozyfireplace.server.util.exception.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final AccountRepository accountRepository;
    private final ProfileRepository profileRepository;
    private final RoomDetailsRepository roomDetailsRepository;
    private final RoomMapper roomMapper;
    private final QuestionRepository questionRepository;

    public void createRoom(RoomCreateRequest request) {
        Profile currentUser = getCurrentAuthenticatedProfile();
        Room room = Room.builder()
                .name(request.getName())
                .url(request.getUrl())
                .description(request.getDescription())
                .build();

        //Создаем аккаунт создателя в комнате
        Account creatorAccount = Account.builder()
                .name(currentUser.getUsername())
                .profile(currentUser)
                .room(room)
                .build();

        creatorAccount = accountRepository.save(creatorAccount);

        room.setCreator(creatorAccount);
        roomRepository.save(room);

        RoomDetails roomDetails = RoomDetails.builder()
                .room(room)
                .build();
        roomDetailsRepository.save(roomDetails);
    }

    public Profile getCurrentAuthenticatedProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }

        String username = authentication.getName();
        return profileRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    public List<RoomContextResponse> getUserRooms() {
        return accountRepository.findByProfile(getCurrentAuthenticatedProfile()).stream()
                .map(account -> {
                    Room room = account.getRoom();
                    boolean isCreator = room.getCreator().getId().equals(account.getId());

                    return RoomContextResponse.builder()
                            .id(room.getId())
                            .name(room.getName())
                            .description(room.getDescription())
                            .url(room.getUrl())
                            .creator(RoomContextResponse.CreatorInfo.builder()
                                    .id(room.getCreator().getId())
                                    .username(room.getCreator().getProfile().getUsername())
                                    .build())
                            .isCurrentUserCreator(isCreator)
                            .memberCount(accountRepository.countByRoom(room))
                            .build();
                })
                .toList();
    }

    public Long getRoomByUrl(String roomUrl) {
        Room room = roomRepository.findByUrl(roomUrl)
                .orElseThrow(() -> new NotFoundException("Room","url", roomUrl));
        return room.getId();
    }

    public RoomContextResponse getRoomById(Long roomId, Account currentAccount) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

        boolean isCreator = room.getCreator().getId().equals(currentAccount.getId());

        return RoomContextResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .url(room.getUrl())
                .creator(RoomContextResponse.CreatorInfo.builder()
                        .id(room.getCreator().getId())
                        .username(room.getCreator().getProfile().getUsername())
                        .build())
                .isCurrentUserCreator(isCreator)
                .memberCount(accountRepository.countByRoom(room))
                .questionCount(questionRepository.countUnansweredByRoomId(roomId))
                .build();
    }

    public void updateRoom (Long roomId, RoomUpdateRequest request) {
        Room room =  roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));
        roomMapper.updateRoomFromRequest(request,room);
        roomRepository.save(room);
    }
}
