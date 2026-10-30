package com.cozyfireplace.server.polls;

import com.cozyfireplace.server.polls.dto.PollCreateRequest;
import com.cozyfireplace.server.polls.dto.PollResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PollController} — a thin facade over {@link PollService} that fixes
 * the create (204) and read-by-source (200) contracts and argument forwarding.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PollController")
class PollControllerTest {

    @Mock
    private PollService pollService;

    @InjectMocks
    private PollController controller;

    @Nested
    @DisplayName("POST")
    class Create {

        @Test
        @DisplayName("returns 204 and forwards the request body")
        void returnsNoContent() {
            PollCreateRequest request = new PollCreateRequest("landing", "text");

            ResponseEntity<Void> response = controller.create(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(pollService).createPoll(request);
        }
    }

    @Nested
    @DisplayName("GET /source")
    class Get {

        @Test
        @DisplayName("returns 200 with the polls for the source")
        void returnsList() {
            List<PollResponse> body = List.of(mock(PollResponse.class));
            when(pollService.getPolls("landing")).thenReturn(body);

            ResponseEntity<List<PollResponse>> response = controller.getPolls("landing");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }
}
