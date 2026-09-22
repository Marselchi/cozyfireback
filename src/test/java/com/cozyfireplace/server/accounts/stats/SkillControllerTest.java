package com.cozyfireplace.server.accounts.stats;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SkillController}.
 * <p>
 * Thin facade over {@link SkillService}: pins the 200 status and body passthrough for the
 * room-scoped skill catalog.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SkillController")
class SkillControllerTest {

    private static final Long ROOM_ID = 1L;

    @Mock
    private SkillService skillService;

    @InjectMocks
    private SkillController controller;

    @Test
    @DisplayName("GET /{roomId} returns the room's skill catalog")
    void getRoomSkills() {
        List<SkillListResponse> body = List.of(mock(SkillListResponse.class));
        when(skillService.getSkills(ROOM_ID)).thenReturn(body);

        ResponseEntity<List<SkillListResponse>> response = controller.getRoomSkills(ROOM_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
    }
}
