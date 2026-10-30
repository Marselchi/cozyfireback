package com.cozyfireplace.server.questions;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.dto.AccountQuestionDataResponse;
import com.cozyfireplace.server.questions.dto.QuestionCreateRequest;
import com.cozyfireplace.server.questions.dto.QuestionDetailResponse;
import com.cozyfireplace.server.questions.dto.QuestionResponse;
import com.cozyfireplace.server.questions.dto.QuestionUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link QuestionController}.
 * <p>
 * Thin HTTP facade over {@link QuestionService}. Each test pins the status code and the
 * forwarding of arguments (note create takes {@code (room, account, request)} but the
 * service is invoked as {@code (request, account, room)}; every other route forwards the
 * {@code roomId} path variable so the service resolves the question inside that room).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionController")
class QuestionControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long QUESTION_ID = 7L;

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private QuestionController controller;

    @Nested
    @DisplayName("POST /{roomId}")
    class Create {

        @Test
        @DisplayName("returns 201 with the created question")
        void returnsCreated() {
            Account account = mock(Account.class);
            QuestionCreateRequest request = new QuestionCreateRequest("t", "b", 2L, null, null);
            QuestionResponse body = mock(QuestionResponse.class);
            when(questionService.createQuestion(request, account, ROOM_ID)).thenReturn(body);

            ResponseEntity<QuestionResponse> response = controller.createQuestion(ROOM_ID, account, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("PUT /{roomId}/{id}")
    class Update {

        @Test
        @DisplayName("returns 200 with the updated question")
        void returnsOk() {
            Account account = mock(Account.class);
            QuestionUpdateRequest request = new QuestionUpdateRequest("t", "b", 3L);
            QuestionResponse body = mock(QuestionResponse.class);
            when(questionService.updateQuestion(ROOM_ID, QUESTION_ID, request, account)).thenReturn(body);

            ResponseEntity<QuestionResponse> response = controller.updateQuestion(ROOM_ID, QUESTION_ID, request, account);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("DELETE /{roomId}/{id}")
    class Delete {

        @Test
        @DisplayName("returns 204 and forwards room, id and account")
        void returnsNoContent() {
            Account account = mock(Account.class);

            ResponseEntity<Void> response = controller.deleteQuestion(ROOM_ID, QUESTION_ID, account);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(questionService).deleteQuestion(ROOM_ID, QUESTION_ID, account);
            verifyNoMoreInteractions(questionService);
        }
    }

    @Nested
    @DisplayName("GET /{roomId}/{id}")
    class Get {

        @Test
        @DisplayName("returns 200 fetching the question inside the room")
        void returnsOk() {
            QuestionResponse body = mock(QuestionResponse.class);
            when(questionService.getQuestionWithReplies(ROOM_ID, QUESTION_ID)).thenReturn(body);

            ResponseEntity<QuestionResponse> response = controller.getQuestion(QUESTION_ID, ROOM_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
            verifyNoMoreInteractions(questionService);
        }
    }

    @Nested
    @DisplayName("GET /{roomId}/{id}/single")
    class Detail {

        @Test
        @DisplayName("returns 200 with the detail and forwarded answer focus")
        void returnsOk() {
            QuestionDetailResponse body = mock(QuestionDetailResponse.class);
            when(questionService.getQuestionById(ROOM_ID, QUESTION_ID, 5L)).thenReturn(body);

            ResponseEntity<QuestionDetailResponse> response = controller.getQuestionById(QUESTION_ID, ROOM_ID, 5L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
            verifyNoMoreInteractions(questionService);
        }
    }

    @Nested
    @DisplayName("GET /{roomId}/authors")
    class Authors {

        @Test
        @DisplayName("returns 200 with the author list")
        void returnsList() {
            List<AccountQuestionDataResponse> body = List.of(new AccountQuestionDataResponse(1L, "a"));
            when(questionService.getQuestionAuthors(ROOM_ID)).thenReturn(body);

            ResponseEntity<List<AccountQuestionDataResponse>> response = controller.getQuestionAuthors(ROOM_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("GET /{roomId}")
    class ListQuestions {

        @Test
        @DisplayName("returns 200 forwarding every filter and the pageable")
        @SuppressWarnings("unchecked")
        void returnsPage() {
            Pageable pageable = mock(Pageable.class);
            Page<QuestionResponse> body = mock(Page.class);
            when(questionService.getQuestions(
                    ROOM_ID, 2, false, null, null, true, null, false, 5L, "gate", pageable)).thenReturn(body);

            ResponseEntity<Page<QuestionResponse>> response = controller.getQuestions(
                    ROOM_ID, 2, false, null, null, true, null, false, 5L, "gate", pageable);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }
}
