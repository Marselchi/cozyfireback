package com.cozyfireplace.server.answers;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.answers.dto.AnswerCreateRequest;
import com.cozyfireplace.server.answers.dto.AnswerResponse;
import com.cozyfireplace.server.answers.dto.AnswerUpdateRequest;
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
 * Unit tests for {@link AnswerController}.
 * <p>
 * The controller is a thin HTTP facade over {@link AnswerService}; each test fixes the
 * status code, the body passthrough and the argument order forwarded to the service
 * (notably {@code createAnswer} re-orders its inputs to {@code (request, account, roomId)}).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnswerController")
class AnswerControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long ANSWER_ID = 100L;
    private static final Long PARENT_ID = 50L;

    @Mock
    private AnswerService answerService;

    @InjectMocks
    private AnswerController controller;

    @Nested
    @DisplayName("POST /{roomId}")
    class Create {

        @Test
        @DisplayName("returns 201 with the created answer")
        void returnsCreated() {
            Account account = mock(Account.class);
            AnswerCreateRequest request = new AnswerCreateRequest("content", 7L, null);
            AnswerResponse body = mock(AnswerResponse.class);
            when(answerService.createAnswer(request, account, ROOM_ID)).thenReturn(body);

            ResponseEntity<AnswerResponse> response = controller.createAnswer(ROOM_ID, request, account);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("PUT /{id}")
    class Update {

        @Test
        @DisplayName("returns 200 with the updated answer")
        void returnsOk() {
            AnswerUpdateRequest request = new AnswerUpdateRequest("edited");
            AnswerResponse body = mock(AnswerResponse.class);
            when(answerService.updateAnswer(ANSWER_ID, request)).thenReturn(body);

            ResponseEntity<AnswerResponse> response = controller.updateAnswer(ANSWER_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("DELETE /{id}")
    class Delete {

        @Test
        @DisplayName("returns 204 and deletes by id")
        void returnsNoContent() {
            ResponseEntity<Void> response = controller.deleteAnswer(ANSWER_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(answerService).deleteAnswer(ANSWER_ID);
        }
    }

    @Nested
    @DisplayName("GET /{id}")
    class Get {

        @Test
        @DisplayName("returns 200 with the answer")
        void returnsOk() {
            AnswerResponse body = mock(AnswerResponse.class);
            when(answerService.getAnswer(ANSWER_ID)).thenReturn(body);

            ResponseEntity<AnswerResponse> response = controller.getAnswer(ANSWER_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("GET /question/{id}")
    class QuestionAnswers {

        @Test
        @DisplayName("returns 200 with the question's answers")
        void returnsList() {
            List<AnswerResponse> body = List.of(mock(AnswerResponse.class));
            when(answerService.getQuestionAnswers(ANSWER_ID)).thenReturn(body);

            ResponseEntity<List<AnswerResponse>> response = controller.getQuestionAnswers(ANSWER_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("GET /{parentId}/children")
    class Children {

        @Test
        @DisplayName("returns 200 with the child answers")
        void returnsList() {
            List<AnswerResponse> body = List.of(mock(AnswerResponse.class));
            when(answerService.getChildAnswers(PARENT_ID)).thenReturn(body);

            ResponseEntity<List<AnswerResponse>> response = controller.getChildAnswers(PARENT_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }
}
