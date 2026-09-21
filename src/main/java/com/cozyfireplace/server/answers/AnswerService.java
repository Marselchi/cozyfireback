package com.cozyfireplace.server.answers;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.answers.dto.*;
import com.cozyfireplace.server.notifications.event.OperationType;
import com.cozyfireplace.server.notifications.event.QAEvent;
import com.cozyfireplace.server.questions.Question;
import com.cozyfireplace.server.questions.QuestionRepository;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import com.cozyfireplace.server.rooms.dto.RoomTextResponse;
import com.cozyfireplace.server.util.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final AnswerMapper answerMapper;
    private final RoomSecurityService roomSecurityService;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomRepository roomRepository;

    /**
     * Создание ответа на вопрос или другого ответа
     * Правила:
     * - Если есть questionId, parentId быть не должно
     * - Если нет questionId, parentId обязателен
     */
    @Transactional
    public AnswerResponse createAnswer(AnswerCreateRequest request, Account account, Long roomId) {
        // Валидация: должен быть либо вопрос, либо родительский ответ
        if (request.questionId() == null && request.parentId() == null) {
            throw new IllegalArgumentException("Either questionId or parentId must be provided");
        }

        RoomTextResponse room = roomRepository.findRoomUrlById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

        boolean isAdmin = roomSecurityService.isCreator(account);

        Answer.AnswerBuilder builder = Answer.builder()
                .content(request.content())
                .author(account);

        // Если есть questionId - создаем ответ на вопрос
        if (request.questionId() != null) {
            Question question = questionRepository.findById(request.questionId())
                    .orElseThrow(() -> new NotFoundException("Question", request.questionId()));
            question.setIsAnswered(isAdmin);
            builder.question(question);
        } else {
            // Если нет questionId, должен быть parentId - создаем ответ на ответ
            Answer parent = answerRepository.findById(request.parentId())
                    .orElseThrow(() -> new NotFoundException("Answer", request.parentId()));
            builder.parent(parent);
            // Также унаследуем вопрос от родительского ответа
            builder.question(parent.getQuestion());
        }

        Answer answer = builder.build();
        Long replyToAccountId = determineReplyTargetAuthor(request);

        Answer saved = answerRepository.save(answer);
        eventPublisher.publishEvent(
                QAEvent.builder()
                        .type(request.parentId() != null ? OperationType.ANSWER : OperationType.QUESTION_ANSWER)
                        .roomId(roomId)
                        .roomUrl(room.getRoomUrl())
                        .roomName(room.getRoomName())
                        .authorAccountId(account.getId())
                        .authorName(account.getName())
                        .questionId(answer.getQuestion().getId())
                        .answerId(saved.getId())
                        .replyToId(answer.getParent() != null ? answer.getParent().getId() : null)
                        .replyToAccountId(replyToAccountId)
                        .byDm(isAdmin)
                        .build()
        );
        return answerMapper.toAnswerResponse(saved, 0, isAdmin);
    }

    private Long determineReplyTargetAuthor(AnswerCreateRequest request) {
        if (request.parentId() != null) {
            return answerRepository.findAuthorIdByAnswerId(request.parentId())
                    .orElse(null);
        }

        if (request.questionId() != null) {
            return questionRepository.findAuthorIdByQuestionId(request.questionId())
                    .orElse(null);
        }

        return null;
    }

    /**
     * Редактирование ответа (только контент)
     */
    @Transactional
    public AnswerResponse updateAnswer(Long id, AnswerUpdateRequest request) {
        Answer answer = answerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Answer", id));

        answer.setContent(request.content());
        Answer updated = answerRepository.save(answer);
        // Получаем количество ответов для обновленного ответа одним запросом
        AnswerWithReplyCount answerWithCount = answerRepository.findByIdWithReplyCount(updated.getId())
                .orElseThrow(() -> new NotFoundException("Answer", updated.getId()));
        return answerMapper.toAnswerResponse(answerWithCount);
    }

    /**
     * Удаление ответа (каскадно удаляет все дочерние ответы)
     */
    @Transactional
    public void deleteAnswer(Long id) {
        if (!answerRepository.existsById(id)) {
            throw new NotFoundException("Answer", id);
        }
        answerRepository.deleteById(id);
    }

    /**
     * Получение дочерних ответов (следующий уровень вложенности)
     */
    @Transactional(readOnly = true)
    public List<AnswerResponse> getChildAnswers(Long parentId) {
        if (!answerRepository.existsById(parentId)) {
            throw new NotFoundException("Answer", parentId);
        }

        List<AnswerWithReplyCount> childAnswersWithCount = answerRepository.findChildAnswersWithReplyCount(parentId);
        return answerMapper.toAnswerResponseListWithReplyCount(childAnswersWithCount);
    }

    @Transactional(readOnly = true)
    public List<AnswerResponse> getQuestionAnswers(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new NotFoundException("Question", questionId);
        }

        List<AnswerWithReplyCount> answersWithCount = answerRepository.findQuestionAnswersWithReplyCount(questionId);
        return answerMapper.toAnswerResponseListWithReplyCount(answersWithCount);
    }

    /**
     * Получение ответа по ID
     */
    @Transactional(readOnly = true)
    public AnswerResponse getAnswer(Long id) {
        AnswerWithReplyCount answerWithCount = answerRepository.findByIdWithReplyCount(id)
                .orElseThrow(() -> new NotFoundException("Answer", id));
        return answerMapper.toAnswerResponse(answerWithCount);
    }
}
