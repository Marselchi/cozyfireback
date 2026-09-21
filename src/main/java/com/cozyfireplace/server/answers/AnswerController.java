package com.cozyfireplace.server.answers;


import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.answers.dto.AnswerCreateRequest;
import com.cozyfireplace.server.answers.dto.AnswerResponse;
import com.cozyfireplace.server.answers.dto.AnswerUpdateRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/answers")
@RequiredArgsConstructor
@Tag(name = "Answer", description = "API для управления ответами")
public class AnswerController {

    private final AnswerService answerService;

    /**
     * Создание ответа на вопрос или другого ответа
     * <br/>
     * Параметры:
     * - questionId: ID вопроса (если это ответ на вопрос)
     * - parentId: ID родительского ответа (если это ответ на ответ)
     * <br/>
     * Правила:
     * - Должен быть указан либо questionId, либо parentId
     * - Нельзя указать оба параметра одновременно
     */
    @PostMapping("/{roomId}")
    public ResponseEntity<AnswerResponse> createAnswer(
            @PathVariable Long roomId,
            @Valid @RequestBody AnswerCreateRequest request,
            @CurrentAccount Account currentAccount
    ) {
        AnswerResponse response = answerService.createAnswer(request, currentAccount, roomId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * TODO roomid check
     * Редактирование ответа (только контент)
     */
    @PutMapping("/{id}")
    public ResponseEntity<AnswerResponse> updateAnswer(
            @PathVariable Long id,
            @Valid @RequestBody AnswerUpdateRequest request
    ) {
        AnswerResponse response = answerService.updateAnswer(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаление ответа (каскадно удаляет все дочерние ответы)
     * TODO roomid check
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnswer(@PathVariable Long id) {
        answerService.deleteAnswer(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получение ответа по ID
     * TODO roomid check
     */
    @GetMapping("/{id}")
    public ResponseEntity<AnswerResponse> getAnswer(@PathVariable Long id) {
        AnswerResponse response = answerService.getAnswer(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Получение ответов по ID вопроса
     */
    @GetMapping("/question/{id}")
    public ResponseEntity<List<AnswerResponse>> getQuestionAnswers(@PathVariable Long id) {
        List<AnswerResponse> responses = answerService.getQuestionAnswers(id);
        return ResponseEntity.ok(responses);
    }

    /**
     * Получение дочерних ответов (следующий уровень вложенности)
     */
    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<AnswerResponse>> getChildAnswers(@PathVariable Long parentId) {
        List<AnswerResponse> responses = answerService.getChildAnswers(parentId);
        return ResponseEntity.ok(responses);
    }
}
