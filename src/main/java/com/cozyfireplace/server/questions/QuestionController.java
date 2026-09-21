package com.cozyfireplace.server.questions;


import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.accounts.dto.AccountQuestionDataResponse;
import com.cozyfireplace.server.questions.dto.QuestionCreateRequest;
import com.cozyfireplace.server.questions.dto.QuestionDetailResponse;
import com.cozyfireplace.server.questions.dto.QuestionResponse;
import com.cozyfireplace.server.questions.dto.QuestionUpdateRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Path;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "Question", description = "API для управления вопросами")
public class QuestionController {

    private final QuestionService questionService;

    /**
     * Создание нового вопроса
     */
    @PostMapping("/{roomId}")
    public ResponseEntity<QuestionResponse> createQuestion(
            @PathVariable Long roomId,
            @CurrentAccount Account account,
            @Valid @RequestBody QuestionCreateRequest request

    ) {
        QuestionResponse response = questionService.createQuestion(request, account, roomId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Обновление вопроса по ID
     */
    @PutMapping("/{id}")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionUpdateRequest request
    ) {
        QuestionResponse response = questionService.updateQuestion(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаление вопроса по ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получение вопроса по ID с ответами первого уровня
     * TODO roomid check
     */
    @GetMapping("/{roomId}/{id}")
    public ResponseEntity<QuestionResponse> getQuestion(@PathVariable Long id, @PathVariable Long roomId) {
        QuestionResponse response = questionService.getQuestionWithReplies(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomId}/{id}/single")
    public ResponseEntity<QuestionDetailResponse> getQuestionById(
            @PathVariable Long id,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long answerId) {
        return ResponseEntity.ok(questionService.getQuestionById(id, answerId));
    }

    /**
     * Получение авторов вопросов
     * TODO roomid check
     */
    @GetMapping("/{roomId}/authors")
    public ResponseEntity<List<AccountQuestionDataResponse>> getQuestionAuthors(@PathVariable Long roomId) {
        List<AccountQuestionDataResponse> response = questionService.getQuestionAuthors(roomId);
        return ResponseEntity.ok(response);
    }

    /**
     * Получение списка вопросов с фильтрацией и пагинацией
     * Параметры фильтрации:
     * - categoryId: фильтр по категории (0-6)
     * - isAnswered: фильтр по статусу ответа (true/false)
     * - loreId: фильтр по конкретному lore ID
     * - characterId: фильтр по конкретному character ID
     * - hasLore: фильтр по наличию lore (true/false)
     * - hasCharacter: фильтр по наличию character (true/false)
     * - allTypes: если true, возвращает вопросы и с lore, и без lore
     * - authorId: фильтр по автору
     * - search: поиск по заголовку вопроса
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<Page<QuestionResponse>> getQuestions(
            @PathVariable Long roomId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean isAnswered,
            @RequestParam(required = false) Long loreId,
            @RequestParam(required = false) Long characterId,
            @RequestParam(required = false) Boolean hasLore,
            @RequestParam(required = false) Boolean hasCharacter,
            @RequestParam(required = false) Boolean allTypes,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 6) Pageable pageable
    ) {
        Page<QuestionResponse> response = questionService.getQuestions(
                roomId, categoryId, isAnswered, loreId, characterId, hasLore, hasCharacter, allTypes, authorId, search, pageable
        );
        return ResponseEntity.ok(response);
    }
}
