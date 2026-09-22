package com.cozyfireplace.server.questions;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.dto.AccountMapper;
import com.cozyfireplace.server.accounts.dto.AccountQuestionDataResponse;
import com.cozyfireplace.server.answers.AnswerRepository;
import com.cozyfireplace.server.answers.dto.AnswerMapper;
import com.cozyfireplace.server.answers.dto.AnswerPathProjection;
import com.cozyfireplace.server.answers.dto.AnswerWithRepliesResponse;
import com.cozyfireplace.server.answers.dto.AnswerWithReplyCount;
import com.cozyfireplace.server.characters.Character;
import com.cozyfireplace.server.characters.CharacterRepository;
import com.cozyfireplace.server.lore.Lore;
import com.cozyfireplace.server.lore.LoreRepository;
import com.cozyfireplace.server.notifications.event.QAEvent;
import com.cozyfireplace.server.questions.dto.QuestionCreateRequest;
import com.cozyfireplace.server.questions.dto.QuestionDetailResponse;
import com.cozyfireplace.server.questions.dto.QuestionMapper;
import com.cozyfireplace.server.questions.dto.QuestionResponse;
import com.cozyfireplace.server.questions.dto.QuestionUpdateRequest;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.dto.RoomTextResponse;
import com.cozyfireplace.server.util.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit tests for {@link QuestionService}.
 * <p>
 * All nine collaborators are mocked. The tests concentrate on the service's own logic:
 * the ordered lore / character / room lookups on create, the entity construction (category
 * narrowing, default unanswered flag), the question-detail thread reconstruction and the
 * contradictory-filter normalisation in {@code getQuestions}. MapStruct mapping and JPA
 * projection behaviour are stubbed, not exercised.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionService")
class QuestionServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long QUESTION_ID = 7L;
    private static final Long ACCOUNT_ID = 42L;
    private static final Long LORE_ID = 23L;
    private static final Long CHARACTER_ID = 15L;
    private static final Long ANSWER_ID = 100L;

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private LoreRepository loreRepository;
    @Mock
    private CharacterRepository characterRepository;
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private AnswerMapper answerMapper;

    @InjectMocks
    private QuestionService service;

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
    @DisplayName("createQuestion")
    class Create {

        @Test
        @DisplayName("throws NotFound when the referenced lore is missing")
        void loreNotFound() {
            when(loreRepository.findById(LORE_ID)).thenReturn(Optional.empty());
            QuestionCreateRequest request = new QuestionCreateRequest("t", "b", null, LORE_ID, null);

            assertThatThrownBy(() -> service.createQuestion(request, mock(Account.class), ROOM_ID))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("throws NotFound when the referenced character is missing")
        void characterNotFound() {
            when(characterRepository.findById(CHARACTER_ID)).thenReturn(Optional.empty());
            QuestionCreateRequest request = new QuestionCreateRequest("t", "b", null, null, CHARACTER_ID);

            assertThatThrownBy(() -> service.createQuestion(request, mock(Account.class), ROOM_ID))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("throws NotFound when the room is missing")
        void roomNotFound() {
            when(roomRepository.findRoomUrlById(ROOM_ID)).thenReturn(Optional.empty());
            QuestionCreateRequest request = new QuestionCreateRequest("t", "b", null, null, null);

            assertThatThrownBy(() -> service.createQuestion(request, mock(Account.class), ROOM_ID))
                    .isInstanceOf(NotFoundException.class);
            verify(questionRepository, never()).save(any());
        }

        @Test
        @DisplayName("builds an unanswered question, narrows the category and links lore/character")
        void success() {
            Account account = account();
            Lore lore = Lore.builder().id(LORE_ID).build();
            Character character = Character.builder().id(CHARACTER_ID).build();
            when(loreRepository.findById(LORE_ID)).thenReturn(Optional.of(lore));
            when(characterRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(character));
            stubRoom();
            Question saved = Question.builder().id(QUESTION_ID).build();
            when(questionRepository.save(any(Question.class))).thenReturn(saved);
            QuestionResponse response = mock(QuestionResponse.class);
            when(questionMapper.toQuestionResponse(saved)).thenReturn(response);

            QuestionCreateRequest request = new QuestionCreateRequest("title", "body", 2L, LORE_ID, CHARACTER_ID);
            QuestionResponse result = service.createQuestion(request, account, ROOM_ID);

            assertThat(result).isSameAs(response);
            ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
            verify(questionRepository).save(captor.capture());
            Question built = captor.getValue();
            assertThat(built.getTitle()).isEqualTo("title");
            assertThat(built.getCategory()).isEqualTo(2);
            assertThat(built.getIsAnswered()).isFalse();
            assertThat(built.getLore()).isSameAs(lore);
            assertThat(built.getCharacter()).isSameAs(character);
            verify(eventPublisher).publishEvent(any(QAEvent.class));
        }
    }

    @Nested
    @DisplayName("updateQuestion")
    class Update {

        @Test
        @DisplayName("throws NotFound when the question is missing")
        void notFound() {
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.updateQuestion(QUESTION_ID, new QuestionUpdateRequest("t", "b", 1L)))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("applies the DTO onto the entity, saves and maps")
        void success() {
            Question question = Question.builder().id(QUESTION_ID).build();
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
            when(questionRepository.save(question)).thenReturn(question);
            QuestionResponse response = mock(QuestionResponse.class);
            when(questionMapper.toQuestionResponse(question)).thenReturn(response);
            QuestionUpdateRequest request = new QuestionUpdateRequest("t2", "b2", 3L);

            QuestionResponse result = service.updateQuestion(QUESTION_ID, request);

            verify(questionMapper).updateQuestionFromDto(request, question);
            assertThat(result).isSameAs(response);
        }
    }

    @Nested
    @DisplayName("deleteQuestion")
    class Delete {

        @Test
        @DisplayName("throws NotFound when nothing exists")
        void notFound() {
            when(questionRepository.existsById(QUESTION_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.deleteQuestion(QUESTION_ID)).isInstanceOf(NotFoundException.class);
            verify(questionRepository, never()).deleteById(QUESTION_ID);
        }

        @Test
        @DisplayName("deletes when present")
        void deletes() {
            when(questionRepository.existsById(QUESTION_ID)).thenReturn(true);

            service.deleteQuestion(QUESTION_ID);

            verify(questionRepository).deleteById(QUESTION_ID);
        }
    }

    @Nested
    @DisplayName("getQuestionWithReplies")
    class WithReplies {

        @Test
        @DisplayName("throws NotFound when the question is missing")
        void notFound() {
            when(questionRepository.findByIdWithAnswersAndAuthor(QUESTION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getQuestionWithReplies(QUESTION_ID))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("maps the fetched question")
        void maps() {
            Question question = Question.builder().id(QUESTION_ID).build();
            when(questionRepository.findByIdWithAnswersAndAuthor(QUESTION_ID)).thenReturn(Optional.of(question));
            QuestionResponse response = mock(QuestionResponse.class);
            when(questionMapper.toQuestionResponse(question)).thenReturn(response);

            assertThat(service.getQuestionWithReplies(QUESTION_ID)).isSameAs(response);
        }
    }

    @Nested
    @DisplayName("getQuestionById")
    class ById {

        @Test
        @DisplayName("throws NotFound when the question is missing")
        void notFound() {
            when(questionRepository.findByIdWithAuthor(QUESTION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getQuestionById(QUESTION_ID, null))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("without an answer focus returns the root answers and an empty path")
        void withoutAnswer() {
            Question question = Question.builder().id(QUESTION_ID).build();
            when(questionRepository.findByIdWithAuthor(QUESTION_ID)).thenReturn(Optional.of(question));
            AnswerWithReplyCount wc = mock(AnswerWithReplyCount.class);
            when(answerRepository.findQuestionAnswersWithReplyCount(QUESTION_ID)).thenReturn(List.of(wc));
            AnswerWithRepliesResponse root =
                    AnswerWithRepliesResponse.builder().id(1L).replies(new ArrayList<>()).build();
            when(answerMapper.toAnswerWithRepliesResponse(wc)).thenReturn(root);
            QuestionResponse qr = mock(QuestionResponse.class);
            when(questionMapper.toQuestionResponse(question)).thenReturn(qr);

            QuestionDetailResponse result = service.getQuestionById(QUESTION_ID, null);

            assertThat(result.question()).isSameAs(qr);
            assertThat(result.answers()).containsExactly(root);
            assertThat(result.pathToAnswer()).isEmpty();
        }

        @Test
        @DisplayName("with an answer focus stitches the leaf into its root and reports the path")
        void withAnswer() {
            Question question = Question.builder().id(QUESTION_ID).build();
            when(questionRepository.findByIdWithAuthor(QUESTION_ID)).thenReturn(Optional.of(question));
            QuestionResponse qr = mock(QuestionResponse.class);
            when(questionMapper.toQuestionResponse(question)).thenReturn(qr);

            AnswerWithReplyCount wc = mock(AnswerWithReplyCount.class);
            when(answerRepository.findQuestionAnswersWithReplyCount(QUESTION_ID)).thenReturn(List.of(wc));
            when(answerMapper.toAnswerWithRepliesResponse(wc))
                    .thenReturn(AnswerWithRepliesResponse.builder().id(1L).replies(new ArrayList<>()).build());

            // findAnswerPathDetailed returns leaf -> root; the service reverses it to root -> leaf.
            AnswerPathProjection leaf = projection(2L);
            AnswerPathProjection root = projection(1L);
            when(answerRepository.findAnswerPathDetailed(ANSWER_ID))
                    .thenReturn(new ArrayList<>(List.of(leaf, root)));

            QuestionDetailResponse result = service.getQuestionById(QUESTION_ID, ANSWER_ID);

            assertThat(result.pathToAnswer()).containsExactly(1L);
            assertThat(result.answers()).hasSize(1);
            assertThat(result.answers().getFirst().id()).isEqualTo(1L);
            assertThat(result.answers().getFirst().replies()).extracting(AnswerWithRepliesResponse::id)
                    .containsExactly(2L);
        }

        private AnswerPathProjection projection(long id) {
            AnswerPathProjection p = mock(AnswerPathProjection.class);
            when(p.getId()).thenReturn(id);
            when(p.getContent()).thenReturn("c" + id);
            when(p.getAuthorId()).thenReturn(9L);
            when(p.getUsername()).thenReturn("u" + id);
            when(p.getCreatedAt()).thenReturn(Instant.EPOCH);
            when(p.getUpdatedAt()).thenReturn(Instant.EPOCH);
            when(p.getIsAdmin()).thenReturn(false);
            when(p.getReplyCount()).thenReturn(0);
            return p;
        }
    }

    @Nested
    @DisplayName("getQuestionAuthors")
    class Authors {

        @Test
        @DisplayName("maps every distinct author through the account mapper")
        void maps() {
            Account author = mock(Account.class);
            when(questionRepository.findDistinctAuthorsByRoomId(ROOM_ID)).thenReturn(List.of(author));
            AccountQuestionDataResponse mapped = new AccountQuestionDataResponse(ACCOUNT_ID, "Nyra");
            when(accountMapper.toAccountQuestionDataResponse(author)).thenReturn(mapped);

            assertThat(service.getQuestionAuthors(ROOM_ID)).containsExactly(mapped);
        }
    }

    @Nested
    @DisplayName("getQuestions")
    class ListQuestions {

        @Test
        @DisplayName("rejects filtering by loreId together with hasLore=false")
        void loreConflict() {
            assertThatThrownBy(() -> service.getQuestions(
                    ROOM_ID, null, null, LORE_ID, null, false, null, null, null, null, mock(Pageable.class)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects filtering by characterId together with hasCharacter=false")
        void characterConflict() {
            assertThatThrownBy(() -> service.getQuestions(
                    ROOM_ID, null, null, null, CHARACTER_ID, null, false, null, null, null, mock(Pageable.class)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("allTypes=true nulls out category and has* filters before querying")
        void allTypesNormalisation() {
            Pageable pageable = mock(Pageable.class);
            Question question = Question.builder().id(QUESTION_ID).build();
            Page<Question> page = new PageImpl<>(List.of(question));
            when(questionRepository.findAllWithFilters(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);
            when(questionMapper.toQuestionResponse(question)).thenReturn(mock(QuestionResponse.class));

            Page<QuestionResponse> result = service.getQuestions(
                    ROOM_ID, 2, false, null, null, true, true, true, 5L, "gate", pageable);

            verify(questionRepository).findAllWithFilters(
                    eq(ROOM_ID), isNull(), eq(false), isNull(), isNull(), isNull(), isNull(), eq(5L), eq("gate"), eq(pageable));
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("without allTypes the has* and category filters are forwarded unchanged")
        void filtersForwarded() {
            Pageable pageable = mock(Pageable.class);
            Page<Question> page = new PageImpl<>(List.of());
            when(questionRepository.findAllWithFilters(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

            service.getQuestions(ROOM_ID, 3, true, null, null, true, null, false, null, "x", pageable);

            verify(questionRepository).findAllWithFilters(
                    eq(ROOM_ID), eq(3), eq(true), isNull(), isNull(), eq(true), isNull(), isNull(), eq("x"), eq(pageable));
        }
    }
}
