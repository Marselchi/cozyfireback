package com.cozyfireplace.server.accounts.stats;

import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.util.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SkillService}.
 * <p>
 * The {@link RoomRepository}, {@link SkillRepository} and {@link SkillMapper} are mocked. Covers
 * the happy projection path and both not-found guards (missing room and a room without a game
 * system, which is what scopes the skill catalog).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SkillService")
class SkillServiceTest {

    private static final Long ROOM_ID = 1L;

    @Mock
    private SkillRepository skillRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private SkillMapper skillMapper;

    @InjectMocks
    private SkillService service;

    @Nested
    @DisplayName("getSkills")
    class GetSkills {

        @Test
        @DisplayName("projects the skills defined for the room's game system")
        void projects() {
            Room room = Room.builder().id(ROOM_ID).gameSystem(GameSystem.DND_5E).build();
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            Skill skill = mock(Skill.class);
            when(skillRepository.findAllBySystem(GameSystem.DND_5E)).thenReturn(List.of(skill));
            SkillListResponse projected = mock(SkillListResponse.class);
            when(skillMapper.toSkillListResponse(skill)).thenReturn(projected);

            assertThat(service.getSkills(ROOM_ID)).containsExactly(projected);
        }

        @Test
        @DisplayName("throws when the room is unknown")
        void roomMissing() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSkills(ROOM_ID))
                    .isInstanceOf(NotFoundException.class);
            verifyNoInteractions(skillRepository, skillMapper);
        }

        @Test
        @DisplayName("throws when the room has no game system")
        void noGameSystem() {
            Room room = Room.builder().id(ROOM_ID).build();
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));

            assertThatThrownBy(() -> service.getSkills(ROOM_ID))
                    .isInstanceOf(NotFoundException.class);
            verifyNoInteractions(skillRepository, skillMapper);
        }
    }
}
