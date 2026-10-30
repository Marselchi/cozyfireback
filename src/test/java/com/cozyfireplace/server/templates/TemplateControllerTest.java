package com.cozyfireplace.server.templates;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.templates.dto.TemplateCreateRequest;
import com.cozyfireplace.server.templates.dto.TemplateDetailResponse;
import com.cozyfireplace.server.templates.dto.TemplateResponse;
import com.cozyfireplace.server.templates.dto.TemplateUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TemplateController}.
 * <p>
 * Thin facade over {@link TemplateService}. Pins the status codes and argument forwarding;
 * notably every mutation forwards the {@code {roomId}} path variable together with the resolved
 * account, so the controller cannot be coaxed into touching another room's template.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateController")
class TemplateControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long TEMPLATE_ID = 2L;

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private TemplateController controller;

    @Nested
    @DisplayName("GET")
    class GetAll {

        @Test
        @DisplayName("returns 200 with the searched page")
        @SuppressWarnings("unchecked")
        void returnsPage() {
            Pageable pageable = mock(Pageable.class);
            Page<TemplateResponse> body = mock(Page.class);
            when(templateService.getAllTemplates("npc", pageable)).thenReturn(body);

            ResponseEntity<Page<TemplateResponse>> response = controller.getAll("npc", pageable);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("GET /{templateId}")
    class GetById {

        @Test
        @DisplayName("returns 200 with the detail")
        void returnsOk() {
            TemplateDetailResponse body = mock(TemplateDetailResponse.class);
            when(templateService.getTemplateById(TEMPLATE_ID)).thenReturn(body);

            ResponseEntity<TemplateDetailResponse> response = controller.getById(TEMPLATE_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("POST /{roomId}")
    class Create {

        @Test
        @DisplayName("returns 204 and authors the template with the account resolved for this room")
        void returnsNoContent() {
            Account account = mock(Account.class);
            TemplateCreateRequest request = new TemplateCreateRequest("NPC", "body");

            ResponseEntity<Void> response = controller.create(ROOM_ID, account, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(templateService).createTemplate(request, account);
        }
    }

    @Nested
    @DisplayName("PUT /{roomId}/{templateId}")
    class Update {

        @Test
        @DisplayName("returns 204 and forwards the room, template id and account")
        void returnsNoContent() {
            Account account = mock(Account.class);
            TemplateUpdateRequest request = new TemplateUpdateRequest("renamed");

            ResponseEntity<Void> response = controller.update(ROOM_ID, TEMPLATE_ID, account, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(templateService).updateTemplateName(ROOM_ID, TEMPLATE_ID, request, account);
            verifyNoMoreInteractions(templateService);
        }
    }

    @Nested
    @DisplayName("DELETE /{roomId}/{templateId}")
    class Delete {

        @Test
        @DisplayName("returns 204 and deletes within the room from the path")
        void returnsNoContent() {
            Account account = mock(Account.class);

            ResponseEntity<Void> response = controller.delete(ROOM_ID, TEMPLATE_ID, account);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(templateService).deleteTemplate(ROOM_ID, TEMPLATE_ID, account);
        }
    }
}
