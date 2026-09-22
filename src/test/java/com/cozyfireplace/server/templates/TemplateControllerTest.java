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
 * notably {@code create} ignores the room id and instead passes the current account's id as
 * the author, and the room id is unused on every endpoint here.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateController")
class TemplateControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long TEMPLATE_ID = 2L;
    private static final Long ACCOUNT_ID = 42L;

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
        @DisplayName("returns 204 and authors the template with the current account id")
        void returnsNoContent() {
            Account account = mock(Account.class);
            when(account.getId()).thenReturn(ACCOUNT_ID);
            TemplateCreateRequest request = new TemplateCreateRequest("NPC", "body");

            ResponseEntity<Void> response = controller.create(ROOM_ID, account, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(templateService).createTemplate(request, ACCOUNT_ID);
        }
    }

    @Nested
    @DisplayName("PUT /{templateId}")
    class Update {

        @Test
        @DisplayName("returns 204 and forwards the rename request")
        void returnsNoContent() {
            TemplateUpdateRequest request = new TemplateUpdateRequest("renamed");

            ResponseEntity<Void> response = controller.update(TEMPLATE_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(templateService).updateTemplateName(TEMPLATE_ID, request);
            verifyNoMoreInteractions(templateService);
        }
    }

    @Nested
    @DisplayName("DELETE /{templateId}")
    class Delete {

        @Test
        @DisplayName("returns 204 and deletes by id")
        void returnsNoContent() {
            ResponseEntity<Void> response = controller.delete(TEMPLATE_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(templateService).deleteTemplate(TEMPLATE_ID);
        }
    }
}
