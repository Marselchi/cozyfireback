package com.cozyfireplace.server.rooms.roomDetails;

import com.cozyfireplace.server.rooms.dto.RoomDetailsChangeRequest;
import com.cozyfireplace.server.rooms.dto.RoomDetailsResponse;
import com.cozyfireplace.server.rooms.dto.RoomMapper;
import com.cozyfireplace.server.util.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RoomDetailsService}.
 * <p>
 * The {@link RoomDetailsRepository} and {@link RoomMapper} are mocked. Covers the
 * read/update flows, delegating mapping to the mapper, and the shared not-found path.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoomDetailsService")
class RoomDetailsServiceTest {

    private static final Long ROOM_ID = 1L;

    @Mock
    private RoomDetailsRepository roomDetailsRepository;
    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomDetailsService service;

    @Nested
    @DisplayName("updateRoomDetails")
    class Update {

        @Test
        @DisplayName("maps onto the stored details and saves")
        void mapsAndSaves() {
            RoomDetails details = mock(RoomDetails.class);
            when(roomDetailsRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(details));
            RoomDetailsChangeRequest request = mock(RoomDetailsChangeRequest.class);

            service.updateRoomDetails(request, ROOM_ID);

            verify(roomMapper).updateRoomDetailsFromRequest(request, details);
            verify(roomDetailsRepository).save(details);
        }

        @Test
        @DisplayName("throws NotFound for an unknown room without saving")
        void notFound() {
            when(roomDetailsRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.empty());
            RoomDetailsChangeRequest request = mock(RoomDetailsChangeRequest.class);

            assertThatThrownBy(() -> service.updateRoomDetails(request, ROOM_ID))
                    .isInstanceOf(NotFoundException.class);
            verifyNoInteractions(roomMapper);
        }
    }

    @Nested
    @DisplayName("getRoomDetails")
    class Get {

        @Test
        @DisplayName("returns the mapper projection")
        void projects() {
            RoomDetails details = mock(RoomDetails.class);
            when(roomDetailsRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(details));
            RoomDetailsResponse response = mock(RoomDetailsResponse.class);
            when(roomMapper.toRoomDetailsResponseFromDetails(details)).thenReturn(response);

            assertThat(service.getRoomDetails(ROOM_ID)).isSameAs(response);
        }

        @Test
        @DisplayName("throws NotFound for an unknown room")
        void notFound() {
            when(roomDetailsRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRoomDetails(ROOM_ID))
                    .isInstanceOf(NotFoundException.class);
            verifyNoInteractions(roomMapper);
        }
    }
}
