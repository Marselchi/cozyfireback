package com.cozyfireplace.server.questions;


import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.accounts.dto.AccountQuestionDataResponse;
import com.cozyfireplace.server.questions.dto.QuestionCreateRequest;
import com.cozyfireplace.server.questions.dto.QuestionDetailResponse;
import com.cozyfireplace.server.questions.dto.QuestionResponse;
import com.cozyfireplace.server.questions.dto.QuestionUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Questions", description = "Q&A: create and manage questions and their threaded answers")
public class QuestionController {

    private final QuestionService questionService;

    @Operation(
            summary = "Create a question",
            description = "Creates a new question in the room, optionally linked to a lore entry and/or a character.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Question created",
                            content = @Content(examples = @ExampleObject(name = "Question", value = """
                                    {
                                      "id": 7,
                                      "title": "Who guards the gate?",
                                      "body": "Any lore on the gate's sentries?",
                                      "category": 2,
                                      "isAnswered": false,
                                      "answers": []
                                    }
                                    """))),
                    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "QuestionCreate", value = """
                    {
                      "title": "Who guards the gate?",
                      "body": "Any lore on the gate's sentries?",
                      "category": 2,
                      "loreId": 23,
                      "characterId": 15
                    }
                    """))
    )
    @PostMapping("/{roomId}")
    public ResponseEntity<QuestionResponse> createQuestion(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account,
            @Valid @RequestBody QuestionCreateRequest request

    ) {
        QuestionResponse response = questionService.createQuestion(request, account, roomId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Update a question",
            description = "Updates a question's title, body and category by its ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Question updated"),
                    @ApiResponse(responseCode = "404", description = "Question not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "QuestionUpdate", value = """
                    { "title": "Who guards the north gate?", "body": "Updated details about the sentries.", "category": 3 }
                    """))
    )
    @PutMapping("/{id}")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @Parameter(description = "ID of the question", required = true, example = "7") @PathVariable Long id,
            @Valid @RequestBody QuestionUpdateRequest request
    ) {
        QuestionResponse response = questionService.updateQuestion(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete a question",
            description = "Deletes a question by its ID.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Question deleted"),
                    @ApiResponse(responseCode = "404", description = "Question not found", content = @Content)
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(
            @Parameter(description = "ID of the question", required = true, example = "7") @PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get a question with first-level answers",
            description = "Returns a question by its ID together with its first-level answers.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Question returned"),
                    @ApiResponse(responseCode = "404", description = "Question not found", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{id}")
    @SuppressWarnings("unused")
    public ResponseEntity<QuestionResponse> getQuestion(
            @Parameter(description = "ID of the question", required = true, example = "7") @PathVariable Long id,
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId) {
        QuestionResponse response = questionService.getQuestionWithReplies(id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get a single question detail",
            description = "Returns detailed information for one question, optionally scoped to a specific answer thread.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Question detail returned"),
                    @ApiResponse(responseCode = "404", description = "Question not found", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{id}/single")
    @SuppressWarnings("unused")
    public ResponseEntity<QuestionDetailResponse> getQuestionById(
            @Parameter(description = "ID of the question", required = true, example = "7") @PathVariable Long id,
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "Optional answer ID to focus the thread on") @RequestParam(required = false) Long answerId) {
        return ResponseEntity.ok(questionService.getQuestionById(id, answerId));
    }

    @Operation(
            summary = "Get question authors",
            description = "Returns the distinct authors who have asked questions in the room.",
            responses = @ApiResponse(responseCode = "200", description = "List of authors returned")
    )
    @GetMapping("/{roomId}/authors")
    public ResponseEntity<List<AccountQuestionDataResponse>> getQuestionAuthors(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId) {
        List<AccountQuestionDataResponse> response = questionService.getQuestionAuthors(roomId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List questions with filters and pagination",
            description = "Returns a page of questions filtered by category, answer status, linked lore/character, author and free-text search.",
            responses = @ApiResponse(responseCode = "200", description = "Page of questions returned")
    )
    @GetMapping("/{roomId}")
    public ResponseEntity<Page<QuestionResponse>> getQuestions(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "Filter by category (0-6)") @RequestParam(required = false) Integer categoryId,
            @Parameter(description = "Filter by answered status") @RequestParam(required = false) Boolean isAnswered,
            @Parameter(description = "Filter by linked lore ID") @RequestParam(required = false) Long loreId,
            @Parameter(description = "Filter by linked character ID") @RequestParam(required = false) Long characterId,
            @Parameter(description = "Only questions that have linked lore") @RequestParam(required = false) Boolean hasLore,
            @Parameter(description = "Only questions that have a linked character") @RequestParam(required = false) Boolean hasCharacter,
            @Parameter(description = "If true, return questions both with and without lore") @RequestParam(required = false) Boolean allTypes,
            @Parameter(description = "Filter by author account ID") @RequestParam(required = false) Long authorId,
            @Parameter(description = "Search by question title", example = "gate") @RequestParam(required = false) String search,
            @Parameter(hidden = true) @PageableDefault(size = 6) Pageable pageable
    ) {
        Page<QuestionResponse> response = questionService.getQuestions(
                roomId, categoryId, isAnswered, loreId, characterId, hasLore, hasCharacter, allTypes, authorId, search, pageable
        );
        return ResponseEntity.ok(response);
    }
}
