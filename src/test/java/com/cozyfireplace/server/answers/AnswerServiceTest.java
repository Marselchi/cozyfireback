package com.cozyfireplace.server.answers;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.answers.dto.AnswerCreateRequest;
import com.cozyfireplace.server.answers.dto.AnswerMapper;
import com.cozyfireplace.server.answers.dto.AnswerResponse;
import com.cozyfireplace.server.answers.dto.AnswerUpdateRequest;
import com.cozyfireplace.server.answers.dto.AnswerWithReplyCount;
import com.cozyfireplace.server.notifications.event.QAEvent;
import com.cozyfireplace.server.questions.Question;
import com.cozyfireplace.server.questions.QuestionRepository;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import com.cozyfireplace.server.rooms.dto.RoomTextResponse;
import com.cozyfireplace.server.util.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit tests for {@link AnswerService}.
 * <p>
 * Every collaborator (repositories, the MapStruct mapper, the room security gate and the
 * event publisher) is mocked, so the tests focus on the service's own branching: the
 * "either questionId or parentId" validation, the question vs. reply creation paths, the
 * DM/admin flag propagation and the not-found guards on each read/write. Persistence and
 * projection semantics are out of scope.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnswerService")
class AnswerServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long ACCOUNT_ID = 42L;
    private static final Long ANSWER_ID = 100L;
    private static final Long QUESTION_ID = 7L;
    private static final Long PARENT_ID = 50L;

    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AnswerMapper answerMapper;
    @Mock
    private RoomSecurityService roomSecurityService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private AnswerService service;

    private Account account() {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(ACCOUNT_ID);
        when(account.getName()).thenReturn("Nyra");
        return account;
    }

    private void stubRoom() {
        RoomTextResponse room = mock(RoomTextResponse.class);
        when(room.getRoomUrl()).thenReturn("/test-room");
        when(room.getRoomName()).thenReturn("Test Room");
        when(roomRepository.findRoomUrlById(ROOM_ID)).thenReturn(Optional.of(room));
    }

    @Nested
    @DisplayName("createAnswer")
    class Create {

        @Test
        @DisplayName("rejects when neither questionId nor parentId is present")
        void requiresQuestionOrParent() {
            AnswerCreateRequest request = new AnswerCreateRequest("content", null, null);

            assertThatThrownBy(() -> service.createAnswer(request, mock(Account.class), ROOM_ID))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(roomRepository, answerRepository, questionRepository);
        }

        @Test
        @DisplayName("throws NotFound when the room does not exist")
        void roomNotFound() {
            when(roomRepository.findRoomUrlById(ROOM_ID)).thenReturn(Optional.empty());
            AnswerCreateRequest request = new AnswerCreateRequest("content", QUESTION_ID, null);

            assertThatThrownBy(() -> service.createAnswer(request, mock(Account.class), ROOM_ID))
                    .isInstanceOf(NotFoundException.class);
            verify(answerRepository, never()).save(any());
        }

        @Test
        @DisplayName("answer to a question links the question, flags it answered for a DM and publishes an event")
        void toQuestionAsAdmin() {
            Account account = account();
            stubRoom();
            when(roomSecurityService.isCreator(account)).thenReturn(true);
            Question question = Question.builder().id(QUESTION_ID).isAnswered(false).build();
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
            when(questionRepository.findAuthorIdByQuestionId(QUESTION_ID)).thenReturn(Optional.of(9L));
            Answer saved = Answer.builder().id(ANSWER_ID).question(question).build();
            when(answerRepository.save(any(Answer.class))).thenReturn(saved);
            AnswerResponse response = mock(AnswerResponse.class);
            when(answerMapper.toAnswerResponse(saved, 0L, true)).thenReturn(response);

            AnswerCreateRequest request = new AnswerCreateRequest("content", QUESTION_ID, null);
            AnswerResponse result = service.createAnswer(request, account, ROOM_ID);

            assertThat(result).isSameAs(response);
            assertThat(question.getIsAnswered()).isTrue();
            verify(eventPublisher).publishEvent(any(QAEvent.class));
        }

        @Test
        @DisplayName("answer to a question leaves the question unanswered for a non-DM author")
        void toQuestionAsPlayer() {
            Account account = account();
            stubRoom();
            when(roomSecurityService.isCreator(account)).thenReturn(false);
            Question question = Question.builder().id(QUESTION_ID).isAnswered(false).build();
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
            when(questionRepository.findAuthorIdByQuestionId(QUESTION_ID)).thenReturn(Optional.empty());
            Answer saved = Answer.builder().id(ANSWER_ID).question(question).build();
            when(answerRepository.save(any(Answer.class))).thenReturn(saved);
            when(answerMapper.toAnswerResponse(saved, 0L, false)).thenReturn(mock(AnswerResponse.class));

            service.createAnswer(new AnswerCreateRequest("content", QUESTION_ID, null), account, ROOM_ID);

            assertThat(question.getIsAnswered()).isFalse();
        }

        @Test
        @DisplayName("reply to an answer inherits the parent's question and links the parent")
        void toParentAnswer() {
            Account account = account();
            stubRoom();
            when(roomSecurityService.isCreator(account)).thenReturn(false);
            Question question = Question.builder().id(QUESTION_ID).build();
            Answer parent = Answer.builder().id(PARENT_ID).question(question).build();
            when(answerRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));
            when(answerRepository.findAuthorIdByAnswerId(PARENT_ID)).thenReturn(Optional.of(9L));
            Answer saved = Answer.builder().id(ANSWER_ID).parent(parent).question(question).build();
            when(answerRepository.save(any(Answer.class))).thenReturn(saved);
            when(answerMapper.toAnswerResponse(saved, 0L, false)).thenReturn(mock(AnswerResponse.class));

            service.createAnswer(new AnswerCreateRequest("reply", null, PARENT_ID), account, ROOM_ID);

            verify(answerRepository).save(any(Answer.class));
            verify(eventPublisher).publishEvent(any(QAEvent.class));
        }

        @Test
        @DisplayName("throws NotFound when the referenced question is missing")
        void questionNotFound() {
            Account account = mock(Account.class);
            when(roomRepository.findRoomUrlById(ROOM_ID)).thenReturn(Optional.of(mock(RoomTextResponse.class)));
            when(roomSecurityService.isCreator(account)).thenReturn(false);
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.createAnswer(new AnswerCreateRequest("c", QUESTION_ID, null), account, ROOM_ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateAnswer")
    class Update {

        @Test
        @DisplayName("throws NotFound when the answer is missing")
        void notFound() {
            when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAnswer(ANSWER_ID, new AnswerUpdateRequest("new")))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("replaces content, re-reads the reply count and maps it")
        void success() {
            Answer answer = Answer.builder().id(ANSWER_ID).content("old").build();
            when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));
            when(answerRepository.save(answer)).thenReturn(answer);
            AnswerWithReplyCount withCount = mock(AnswerWithReplyCount.class);
            when(answerRepository.findByIdWithReplyCount(ANSWER_ID)).thenReturn(Optional.of(withCount));
            AnswerResponse response = mock(AnswerResponse.class);
            when(answerMapper.toAnswerResponse(withCount)).thenReturn(response);

            AnswerResponse result = service.updateAnswer(ANSWER_ID, new AnswerUpdateRequest("new"));

            assertThat(answer.getContent()).isEqualTo("new");
            assertThat(result).isSameAs(response);
        }
    }

    @Nested
    @DisplayName("deleteAnswer")
    class Delete {

        @Test
        @DisplayName("throws NotFound when nothing exists to delete")
        void notFound() {
            when(answerRepository.existsById(ANSWER_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.deleteAnswer(ANSWER_ID)).isInstanceOf(NotFoundException.class);
            verify(answerRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("deletes when present")
        void deletes() {
            when(answerRepository.existsById(ANSWER_ID)).thenReturn(true);

            service.deleteAnswer(ANSWER_ID);

            verify(answerRepository).deleteById(ANSWER_ID);
        }
    }

    @Nested
    @DisplayName("getChildAnswers")
    class Children {

        @Test
        @DisplayName("throws NotFound when the parent is missing")
        void notFound() {
            when(answerRepository.existsById(PARENT_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.getChildAnswers(PARENT_ID)).isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("maps the child projections when the parent exists")
        void maps() {
            when(answerRepository.existsById(PARENT_ID)).thenReturn(true);
            List<AnswerWithReplyCount> rows = List.of(mock(AnswerWithReplyCount.class));
            when(answerRepository.findChildAnswersWithReplyCount(PARENT_ID)).thenReturn(rows);
            List<AnswerResponse> mapped = List.of(mock(AnswerResponse.class));
            when(answerMapper.toAnswerResponseListWithReplyCount(rows)).thenReturn(mapped);

            assertThat(service.getChildAnswers(PARENT_ID)).isSameAs(mapped);
        }
    }

    @Nested
    @DisplayName("getQuestionAnswers")
    class QuestionAnswers {

        @Test
        @DisplayName("throws NotFound when the question is missing")
        void notFound() {
            when(questionRepository.existsById(QUESTION_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.getQuestionAnswers(QUESTION_ID)).isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("maps the question's answers when it exists")
        void maps() {
            when(questionRepository.existsById(QUESTION_ID)).thenReturn(true);
            List<AnswerWithReplyCount> rows = List.of(mock(AnswerWithReplyCount.class));
            when(answerRepository.findQuestionAnswersWithReplyCount(QUESTION_ID)).thenReturn(rows);
            List<AnswerResponse> mapped = List.of(mock(AnswerResponse.class));
            when(answerMapper.toAnswerResponseListWithReplyCount(rows)).thenReturn(mapped);

            assertThat(service.getQuestionAnswers(QUESTION_ID)).isSameAs(mapped);
        }
    }

    @Nested
    @DisplayName("getAnswer")
    class Get {

        @Test
        @DisplayName("throws NotFound when the answer projection is missing")
        void notFound() {
            when(answerRepository.findByIdWithReplyCount(ANSWER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAnswer(ANSWER_ID)).isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("maps the projection when present")
        void maps() {
            AnswerWithReplyCount withCount = mock(AnswerWithReplyCount.class);
            when(answerRepository.findByIdWithReplyCount(ANSWER_ID)).thenReturn(Optional.of(withCount));
            AnswerResponse response = mock(AnswerResponse.class);
            when(answerMapper.toAnswerResponse(withCount)).thenReturn(response);

            assertThat(service.getAnswer(ANSWER_ID)).isSameAs(response);
        }
    }
}
