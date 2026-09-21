package com.cozyfireplace.server.questions;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.accounts.dto.AccountMapper;
import com.cozyfireplace.server.accounts.dto.AccountQuestionDataResponse;
import com.cozyfireplace.server.answers.Answer;
import com.cozyfireplace.server.answers.AnswerRepository;
import com.cozyfireplace.server.answers.dto.*;
import com.cozyfireplace.server.characters.Character;
import com.cozyfireplace.server.characters.CharacterRepository;
import com.cozyfireplace.server.lore.Lore;
import com.cozyfireplace.server.lore.LoreRepository;
import com.cozyfireplace.server.notifications.event.CharacterEvent;
import com.cozyfireplace.server.notifications.event.OperationType;
import com.cozyfireplace.server.notifications.event.QAEvent;
import com.cozyfireplace.server.questions.dto.*;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.dto.RoomTextResponse;
import com.cozyfireplace.server.util.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final AccountMapper accountMapper;
    private final LoreRepository loreRepository;
    private final CharacterRepository characterRepository;
    private final QuestionMapper questionMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomRepository roomRepository;
    private final AnswerRepository answerRepository;
    private final AnswerMapper answerMapper;

    /**
     * Создание нового вопроса
     */
    @Transactional
    public QuestionResponse createQuestion(QuestionCreateRequest request, Account account, Long roomId) {

        Lore lore = null;
        if (request.loreId() != null) {
            lore = loreRepository.findById(request.loreId())
                    .orElseThrow(() -> new NotFoundException("Lore", request.loreId()));
        }

        Character character = null;
        if (request.characterId() != null) {
            character = characterRepository.findById(request.characterId())
                    .orElseThrow(() -> new NotFoundException("Character", request.characterId()));
        }
        RoomTextResponse room = roomRepository.findRoomUrlById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

        Question question = Question.builder()
                .title(request.title())
                .body(request.body())
                .category(request.category() != null ? request.category().intValue() : null)
                .author(account)
                .lore(lore)
                .character(character)
                .isAnswered(false)
                .build();

        Question saved = questionRepository.save(question);
        eventPublisher.publishEvent(
                QAEvent.builder()
                        .roomUrl(room.getRoomUrl())
                        .roomName(room.getRoomName())
                        .type(OperationType.CREATE)
                        .roomId(roomId)
                        .authorAccountId(account.getId())
                        .authorName(account.getName())
                        .questionId(saved.getId())
                        .build()
        );
        return questionMapper.toQuestionResponse(saved);
    }

    /**
     * Обновление вопроса
     */
    @Transactional
    public QuestionResponse updateQuestion(Long id, QuestionUpdateRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Question", id));

        questionMapper.updateQuestionFromDto(request, question);
        Question updated = questionRepository.save(question);
        return questionMapper.toQuestionResponse(updated);
    }

    /**
     * Удаление вопроса
     */
    @Transactional
    public void deleteQuestion(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new NotFoundException("Question", id);
        }
        questionRepository.deleteById(id);
    }

    /**
     * Получение вопроса по ID с ответами первого уровня
     */
    @Transactional(readOnly = true)
    public QuestionResponse getQuestionWithReplies(Long id) {
        Question question = questionRepository.findByIdWithAnswersAndAuthor(id)
                .orElseThrow(() -> new NotFoundException("Question", id));
        return questionMapper.toQuestionResponse(question);
    }

    @Transactional(readOnly = true)
    public QuestionDetailResponse getQuestionById(Long questionId, Long answerId) {

        Question question = questionRepository.findByIdWithAuthor(questionId)
                .orElseThrow(() -> new NotFoundException("Question", questionId));

        List<AnswerWithRepliesResponse> rootAnswers = new ArrayList<>(answerRepository
                .findQuestionAnswersWithReplyCount(questionId)
                .stream()
                .map(answerMapper::toAnswerWithRepliesResponse)
                .toList());

        if (answerId == null) {
            return new QuestionDetailResponse(
                    questionMapper.toQuestionResponse(question),
                    rootAnswers,
                    List.of()
            );
        }

        List<AnswerPathProjection> path = answerRepository.findAnswerPathDetailed(answerId);

        Collections.reverse(path); // root → leaf

        Map<Long, AnswerWithRepliesResponse> map = getLongAnswerWithRepliesResponseMap(path);

        // связываем
        for (int i = 0; i < path.size() - 1; i++) {
            Long parentId = path.get(i).getId();
            Long childId = path.get(i + 1).getId();

            map.get(parentId).replies().add(map.get(childId));
        }

        // вставляем в root
        Long rootId = path.getFirst().getId();

        for (int i = 0; i < rootAnswers.size(); i++) {
            if (rootAnswers.get(i).id().equals(rootId)) {
                rootAnswers.set(i, map.get(rootId));
                break;
            }
        }

        List<Long> pathToAnswer = path.stream()
                .limit(path.size() - 1)
                .map(AnswerPathProjection::getId)
                .toList();

        return new QuestionDetailResponse(
                questionMapper.toQuestionResponse(question),
                rootAnswers,
                pathToAnswer
        );
    }

    private static @NonNull Map<Long, AnswerWithRepliesResponse> getLongAnswerWithRepliesResponseMap(List<AnswerPathProjection> path) {
        Map<Long, AnswerWithRepliesResponse> map = new HashMap<>();

        for (AnswerPathProjection p : path) {
            map.put(p.getId(), new AnswerWithRepliesResponse(
                    p.getId(),
                    p.getContent(),
                    new AccountQuestionDataResponse(p.getAuthorId(), p.getUsername()),
                    p.getCreatedAt(),
                    p.getUpdatedAt(),
                    p.getIsAdmin(),
                    p.getReplyCount(),
                    new ArrayList<>()
            ));
        }
        return map;
    }

    /**
     * Получение всех авторов вопросов по комнате
     */
    @Transactional(readOnly = true)
    public List<AccountQuestionDataResponse> getQuestionAuthors(Long roomId) {
        List<Account> authors = questionRepository.findDistinctAuthorsByRoomId(roomId);
        return authors.stream().map(accountMapper::toAccountQuestionDataResponse).toList();
    }


    /**
     * Получение списка вопросов с фильтрацией и пагинацией
     *
     * Фильтры:
     * - roomId: ID комнаты для фильтрации вопросов
     * - categoryId: фильтр по категории (0-6)
     * - isAnswered: фильтр по статусу ответа
     * - loreId: фильтр по конкретному lore (если есть, категория игнорируется)
     * - characterId: фильтр по конкретному character
     * - hasLore: фильтр по наличию lore (true/false)
     * - hasCharacter: фильтр по наличию character (true/false)
     * - allTypes: если true, возвращает вопросы и с lore, и без lore (игнорирует hasLore и categoryId)
     * - authorId: фильтр по автору
     * - search: поиск по заголовку вопроса
     */
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getQuestions(
            Long roomId,
            Integer categoryId,
            Boolean isAnswered,
            Long loreId,
            Long characterId,
            Boolean hasLore,
            Boolean hasCharacter,
            Boolean allTypes,
            Long authorId,
            String search,
            Pageable pageable
    ) {
        // Валидация противоречащих фильтров
        if (loreId != null && hasLore != null && !hasLore) {
            throw new IllegalArgumentException("Cannot filter by loreId and hasLore=false at the same time");
        }
        if (characterId != null && hasCharacter != null && !hasCharacter) {
            throw new IllegalArgumentException("Cannot filter by characterId and hasCharacter=false at the same time");
        }

        // Если allTypes=true, игнорируем hasLore, hasCharacter и categoryId
        Boolean effectiveHasLore = (allTypes != null && allTypes) ? null : hasLore;
        Boolean effectiveHasCharacter = (allTypes != null && allTypes) ? null : hasCharacter;
        Integer effectiveCategoryId = (allTypes != null && allTypes) ? null : categoryId;

        Page<Question> page = questionRepository.findAllWithFilters(
                roomId,
                effectiveCategoryId,
                isAnswered,
                loreId,
                characterId,
                effectiveHasLore,
                effectiveHasCharacter,
                authorId,
                search,
                pageable
        );

        return page.map(questionMapper::toQuestionResponse);
    }
}
