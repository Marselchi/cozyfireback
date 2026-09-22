package com.cozyfireplace.server.roles;

import com.cozyfireplace.server.roles.dto.RoleCreateRequest;
import com.cozyfireplace.server.roles.dto.RoleEditResponse;
import com.cozyfireplace.server.roles.dto.RoleResponse;
import com.cozyfireplace.server.roles.dto.RoleUpdateRequest;
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
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Room roles used to scope access to characters and lore")
public class RoleController {
    private final RoleService roleService;

    @Operation(
            summary = "Create a role in a room",
            description = "Creates a role in the room identified by roomId.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Created"),
                    @ApiResponse(responseCode = "404", description = "Room not found", content = @Content),
                    @ApiResponse(responseCode = "409", description = "Role already exists", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "RoleCreate", value = """
                    { "name": "Party Member" }
                    """))
    )
    @PostMapping("/{roomId}")
    public ResponseEntity<Void> create(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId,
            @Valid @RequestBody RoleCreateRequest request
    ) {
        roleService.createRole(roomId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Update a role in a room",
            description = "Updates a role.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Updated"),
                    @ApiResponse(responseCode = "404", description = "Role not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "RoleUpdate", value = """
                    { "name": "Trusted Party Member" }
                    """))
    )
    @PutMapping("/{roomId}/{roleId}")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> update(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId,
            @Parameter(description = "ID of the role", required = true, example = "8")
            @PathVariable Long roleId,
            @Valid @RequestBody RoleUpdateRequest request
    ) {
        roleService.updateRole(roleId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete a role in a room",
            description = "Deletes a role.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Deleted"),
                    @ApiResponse(responseCode = "404", description = "Role not found", content = @Content)
            }
    )
    @DeleteMapping("/{roomId}/{roleId}")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId,
            @Parameter(description = "ID of the role", required = true, example = "8")
            @PathVariable Long roleId
    ) {
        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get a role",
            description = "Returns a RoleResponse.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = RoleResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Role not found", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{roleId}")
    @SuppressWarnings("unused")
    public ResponseEntity<RoleResponse> getRole(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId,
            @Parameter(description = "ID of the role", required = true, example = "8")
            @PathVariable Long roleId
    ) {
        return ResponseEntity.ok(roleService.getRole(roleId));
    }

    @Operation(
            summary = "Get all roles in a room",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of roles",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoleResponse.class))))
            }
    )
    @GetMapping("/{roomId}/all")
    public ResponseEntity<List<RoleResponse>> getAll(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(roleService.getAllRoomRoles(roomId));
    }

    @Operation(
            summary = "Get all roles for editing",
            description = "Returns roles with the extra fields needed by the editing UI.",
            responses = @ApiResponse(responseCode = "200", description = "List of roles for editing")
    )
    @GetMapping("/{roomId}/allEdit")
    public ResponseEntity<List<RoleEditResponse>> getAllEdit(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(roleService.getAllRoomRolesEdit(roomId));
    }
}
