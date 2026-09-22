package com.cozyfireplace.server.roles;

import com.cozyfireplace.server.roles.dto.RoleCreateRequest;
import com.cozyfireplace.server.roles.dto.RoleEditResponse;
import com.cozyfireplace.server.roles.dto.RoleResponse;
import com.cozyfireplace.server.roles.dto.RoleUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RoleController}.
 * <p>
 * The controller is a thin HTTP facade over {@link RoleService}; each test locks
 * the contract: the returned status code, the body passthrough and which path
 * variables actually reach the service (note the room id is accepted but ignored
 * by the role-scoped operations, mirroring the source's {@code @SuppressWarnings}).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleController")
class RoleControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long ROLE_ID = 8L;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleController controller;

    @Nested
    @DisplayName("POST /{roomId}")
    class Create {

        @Test
        @DisplayName("returns 204 and forwards the room id and request body")
        void returnsNoContent() {
            RoleCreateRequest request = new RoleCreateRequest("Party Member");

            ResponseEntity<Void> response = controller.create(ROOM_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(roleService).createRole(ROOM_ID, request);
        }
    }

    @Nested
    @DisplayName("PUT /{roomId}/{roleId}")
    class Update {

        @Test
        @DisplayName("returns 204 and updates by role id (room id is ignored)")
        void returnsNoContent() {
            RoleUpdateRequest request = new RoleUpdateRequest("Trusted Party Member");

            ResponseEntity<Void> response = controller.update(ROOM_ID, ROLE_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(roleService).updateRole(ROLE_ID, request);
            verifyNoMoreInteractions(roleService);
        }
    }

    @Nested
    @DisplayName("DELETE /{roomId}/{roleId}")
    class Delete {

        @Test
        @DisplayName("returns 204 and deletes by role id (room id is ignored)")
        void returnsNoContent() {
            ResponseEntity<Void> response = controller.delete(ROOM_ID, ROLE_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(roleService).deleteRole(ROLE_ID);
        }
    }

    @Nested
    @DisplayName("GET /{roomId}/{roleId}")
    class GetRole {

        @Test
        @DisplayName("returns 200 with the role fetched by id")
        void returnsBody() {
            RoleResponse body = new RoleResponse(ROLE_ID, "Role");
            when(roleService.getRole(ROLE_ID)).thenReturn(body);

            ResponseEntity<RoleResponse> response = controller.getRole(ROOM_ID, ROLE_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("GET listing")
    class Listing {

        @Test
        @DisplayName("GET /{roomId}/all returns the room role list")
        void getAll() {
            List<RoleResponse> body = List.of(new RoleResponse(1L, "a"));
            when(roleService.getAllRoomRoles(ROOM_ID)).thenReturn(body);

            ResponseEntity<List<RoleResponse>> response = controller.getAll(ROOM_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }

        @Test
        @DisplayName("GET /{roomId}/allEdit returns the editable role list")
        void getAllEdit() {
            List<RoleEditResponse> body = List.of(new RoleEditResponse(1L, "a", new String[]{}));
            when(roleService.getAllRoomRolesEdit(ROOM_ID)).thenReturn(body);

            ResponseEntity<List<RoleEditResponse>> response = controller.getAllEdit(ROOM_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }
}
