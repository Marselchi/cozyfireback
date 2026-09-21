package com.cozyfireplace.server.roles;

import com.cozyfireplace.server.roles.dto.RoleCreateRequest;
import com.cozyfireplace.server.roles.dto.RoleEditResponse;
import com.cozyfireplace.server.roles.dto.RoleResponse;
import com.cozyfireplace.server.roles.dto.RoleUpdateRequest;
import com.cozyfireplace.server.tags.TagService;
import com.cozyfireplace.server.tags.dto.TagCreateRequest;
import com.cozyfireplace.server.tags.dto.TagResponse;
import com.cozyfireplace.server.tags.dto.TagUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
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
@Tag(name = "Roles", description = "Роли комнаты")
public class RoleController {
    private final RoleService roleService;

    @Operation(
            summary = "Создать роль в комнате",
            description = "Создаёт роль в комнате roomId",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Создано"),
                    @ApiResponse(responseCode = "404", description = "Комната не найдена", content = @Content),
                    @ApiResponse(responseCode = "409", description = "Роль уже существует", content = @Content)
            }
    )
    @PostMapping("/{roomId}")
    public ResponseEntity<Void> create(
            @Parameter(description = "ID комнаты", required = true)
            @PathVariable Long roomId,
            @Valid @RequestBody RoleCreateRequest request
    ) {
        roleService.createRole(roomId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Обновить роль в комнате",
            description = "Обновляет роль",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Обновлено"),
                    @ApiResponse(responseCode = "404", description = "Роль не найдена", content = @Content)
            }
    )
    @PutMapping("/{roomId}/{roleId}")
    public ResponseEntity<Void> update(
            @Parameter(description = "ID комнаты", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "ID роли", required = true)
            @PathVariable Long roleId,
            @Valid @RequestBody RoleUpdateRequest request
    ) {
        roleService.updateRole(roleId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Удалить роль в комнате",
            description = "Удаляет роль",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Удалено"),
                    @ApiResponse(responseCode = "404", description = "Роль не найдена", content = @Content)
            }
    )
    @DeleteMapping("/{roomId}/{roleId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID комнаты", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "ID роли", required = true)
            @PathVariable Long roleId
    ) {
        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Получить роль",
            description = "Возвращает RoleResponse.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ок",
                            content = @Content(schema = @Schema(implementation = RoleResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Роль не найдена", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{roleId}")
    public ResponseEntity<RoleResponse> getRole(
            @Parameter(description = "ID комнаты", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "ID роли", required = true)
            @PathVariable Long roleId
    ) {
        return ResponseEntity.ok(roleService.getRole(roleId));
    }

    @Operation(
            summary = "Получить все роли комнаты",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список ролей",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoleResponse.class))))
            }
    )
    @GetMapping("/{roomId}/all")
    public ResponseEntity<List<RoleResponse>> getAll(
            @Parameter(description = "ID комнаты", required = true)
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(roleService.getAllRoomRoles(roomId));
    }

    @GetMapping("/{roomId}/allEdit")
    public ResponseEntity<List<RoleEditResponse>> getAllEdit(
            @Parameter(description = "ID комнаты", required = true)
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(roleService.getAllRoomRolesEdit(roomId));
    }
}
