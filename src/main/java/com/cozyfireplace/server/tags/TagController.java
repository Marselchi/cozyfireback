package com.cozyfireplace.server.tags;

import com.cozyfireplace.server.tags.dto.TagCreateRequest;
import com.cozyfireplace.server.tags.dto.TagResponse;
import com.cozyfireplace.server.tags.dto.TagUpdateRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@Tag(name = "Tags", description = "Lore tags used to categorize and filter lore entries")
public class TagController {

    private final TagService tagService;

    @Operation(
            summary = "Create a tag in a room",
            description = "Creates a tag in the room identified by roomId.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Created"),
                    @ApiResponse(responseCode = "404", description = "Room not found", content = @Content),
                    @ApiResponse(responseCode = "409", description = "Tag already exists", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "TagCreate", value = """
                    { "name": "Locations" }
                    """))
    )
    @PostMapping("/{roomId}")
    public ResponseEntity<Void> create(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId,
            @Valid @RequestBody TagCreateRequest request
    ) {
        tagService.createTag(roomId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Update a tag in a room",
            description = "Updates a tag.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Updated"),
                    @ApiResponse(responseCode = "404", description = "Tag not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "TagUpdate", value = """
                    { "name": "Landmarks" }
                    """))
    )
    @PutMapping("/{roomId}/{tagId}")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> update(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId,
            @Parameter(description = "ID of the tag", required = true, example = "4")
            @PathVariable Long tagId,
            @Valid @RequestBody TagUpdateRequest request
    ) {
        tagService.updateTag(tagId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete a tag in a room",
            description = "Deletes a tag.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Deleted"),
                    @ApiResponse(responseCode = "404", description = "Tag not found", content = @Content)
            }
    )
    @DeleteMapping("/{roomId}/{tagId}")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId,
            @Parameter(description = "ID of the tag", required = true, example = "4")
            @PathVariable Long tagId
    ) {
        tagService.deleteTag(tagId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get a tag",
            description = "Returns a TagResponse.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = TagResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Tag not found", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{tagId}")
    @SuppressWarnings("unused")
    public ResponseEntity<TagResponse> getTag(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId,
            @Parameter(description = "ID of the tag", required = true, example = "4")
            @PathVariable Long tagId
    ) {
        return ResponseEntity.ok(tagService.getTag(tagId));
    }

    @Operation(
            summary = "Get all tags in a room",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of tags",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TagResponse.class))))
            }
    )
    @GetMapping("/{roomId}/all")
    public ResponseEntity<List<TagResponse>> getAll(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(tagService.getAllRoomTags(roomId));
    }

}
