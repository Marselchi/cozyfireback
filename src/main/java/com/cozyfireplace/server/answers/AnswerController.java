package com.cozyfireplace.server.answers;


import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.answers.dto.AnswerCreateRequest;
import com.cozyfireplace.server.answers.dto.AnswerResponse;
import com.cozyfireplace.server.answers.dto.AnswerUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/answers")
@RequiredArgsConstructor
@Tag(name = "Answers", description = "Create and manage threaded answers to questions and to other answers")
public class AnswerController {

    private final AnswerService answerService;

    @Operation(
            summary = "Create an answer",
            description = "Creates an answer to a question or to another answer. Exactly one of `questionId` or `parentId` must be provided (never both).",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Answer created",
                            content = @Content(examples = @ExampleObject(name = "Answer", value = """
                                    {
                                      "id": 100,
                                      "content": "The gate was barred from within.",
                                      "author": { "id": 42, "username": "Nyra" },
                                      "createdAt": "2026-09-22T10:15:00Z",
                                      "updatedAt": "2026-09-22T10:15:00Z",
                                      "isAdmin": false,
                                      "replyCount": 0
                                    }
                                    """))),
                    @ApiResponse(responseCode = "400", description = "Both or neither of questionId/parentId provided", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = {
                    @ExampleObject(name = "ToQuestion", value = """
                            { "content": "The gate was barred from within.", "questionId": 7 }
                            """),
                    @ExampleObject(name = "ToAnswer", value = """
                            { "content": "Agreed, and the hinges were rusted.", "parentId": 100 }
                            """)
            })
    )
    @PostMapping("/{roomId}")
    public ResponseEntity<AnswerResponse> createAnswer(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Valid @RequestBody AnswerCreateRequest request,
            @Parameter(hidden = true) @CurrentAccount Account currentAccount
    ) {
        AnswerResponse response = answerService.createAnswer(request, currentAccount, roomId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Update an answer",
            description = "Edits the content of an existing answer.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Answer updated"),
                    @ApiResponse(responseCode = "404", description = "Answer not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "AnswerUpdate", value = """
                    { "content": "The gate was barred from within, we later realized." }
                    """))
    )
    @PutMapping("/{id}")
    public ResponseEntity<AnswerResponse> updateAnswer(
            @Parameter(description = "ID of the answer", required = true, example = "100") @PathVariable Long id,
            @Valid @RequestBody AnswerUpdateRequest request
    ) {
        AnswerResponse response = answerService.updateAnswer(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete an answer",
            description = "Deletes an answer and cascade-deletes all of its child answers.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Answer deleted"),
                    @ApiResponse(responseCode = "404", description = "Answer not found", content = @Content)
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnswer(
            @Parameter(description = "ID of the answer", required = true, example = "100") @PathVariable Long id) {
        answerService.deleteAnswer(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get an answer by ID",
            description = "Returns a single answer by its ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Answer returned"),
                    @ApiResponse(responseCode = "404", description = "Answer not found", content = @Content)
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<AnswerResponse> getAnswer(
            @Parameter(description = "ID of the answer", required = true, example = "100") @PathVariable Long id) {
        AnswerResponse response = answerService.getAnswer(id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get answers for a question",
            description = "Returns the first-level answers for the given question.",
            responses = @ApiResponse(responseCode = "200", description = "List of answers returned")
    )
    @GetMapping("/question/{id}")
    public ResponseEntity<List<AnswerResponse>> getQuestionAnswers(
            @Parameter(description = "ID of the question", required = true, example = "7") @PathVariable Long id) {
        List<AnswerResponse> responses = answerService.getQuestionAnswers(id);
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Get child answers",
            description = "Returns the next nesting level of answers for the given parent answer.",
            responses = @ApiResponse(responseCode = "200", description = "List of child answers returned")
    )
    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<AnswerResponse>> getChildAnswers(
            @Parameter(description = "ID of the parent answer", required = true, example = "100") @PathVariable Long parentId) {
        List<AnswerResponse> responses = answerService.getChildAnswers(parentId);
        return ResponseEntity.ok(responses);
    }
}
