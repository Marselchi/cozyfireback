package com.cozyfireplace.server.templates;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import com.cozyfireplace.server.templates.dto.TemplateCreateRequest;
import com.cozyfireplace.server.templates.dto.TemplateDetailResponse;
import com.cozyfireplace.server.templates.dto.TemplateMapper;
import com.cozyfireplace.server.templates.dto.TemplateResponse;
import com.cozyfireplace.server.templates.dto.TemplateUpdateRequest;
import com.cozyfireplace.server.util.exception.ForbiddenException;
import com.cozyfireplace.server.util.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit tests for {@link TemplateService}.
 * <p>
 * The repository and MapStruct mapper are mocked. Exercises the search-vs-browse branch of
 * listing, the entity construction on create, the DTO-onto-entity update and the not-found
 * guards. Update and delete are room-scoped: the repository lookup is keyed by
 * {@code (roomId, templateId)} — an id from another room answers 404 — and only afterwards does
 * the author-or-creator check run, so a stranger gets 403. JPA pagination internals are supplied
 * via a real {@link PageImpl} so the {@code Page#map} projection is genuinely exercised.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateService")
class TemplateServiceTest {

    private static final Long ROOM_ID = 7L;
    private static final Long TEMPLATE_ID = 2L;
    private static final Long AUTHOR_ID = 42L;

    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private RoomSecurityService roomSecurityService;
    @Mock
    private TemplateMapper templateMapper;

    @InjectMocks
    private TemplateService service;

    /**
     * The service only reads {@code Account#getId()} when comparing the caller to the template
     * author, so a real (unmocked) entity is enough and keeps Mockito's strict stubbing happy.
     */
    private static Account account(Long id) {
        return Account.builder().id(id).build();
    }

    @Nested
    @DisplayName("getAllTemplates")
    class GetAll {

        @Test
        @DisplayName("searches by name when a non-blank term is supplied")
        void withSearch() {
            Pageable pageable = mock(Pageable.class);
            Template template = Template.builder().id(TEMPLATE_ID).build();
            when(templateRepository.findByNameContainingIgnoreCase("npc", pageable))
                    .thenReturn(new PageImpl<>(List.of(template)));
            TemplateResponse mapped = mock(TemplateResponse.class);
            when(templateMapper.toTemplateResponse(template)).thenReturn(mapped);

            Page<TemplateResponse> result = service.getAllTemplates("npc", pageable);

            assertThat(result.getContent()).containsExactly(mapped);
            verify(templateRepository, never()).findAll(pageable);
        }

        @Test
        @DisplayName("lists everything when the search term is null or blank")
        void blankSearch() {
            Pageable pageable = mock(Pageable.class);
            when(templateRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

            assertThat(service.getAllTemplates("   ", pageable)).isEmpty();
            verify(templateRepository, never()).findByNameContainingIgnoreCase(any(), any());
        }
    }

    @Nested
    @DisplayName("getTemplateById")
    class GetById {

        @Test
        @DisplayName("throws NotFound when the template is missing")
        void notFound() {
            when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTemplateById(TEMPLATE_ID)).isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("maps the detail response when present")
        void maps() {
            Template template = Template.builder().id(TEMPLATE_ID).build();
            when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
            TemplateDetailResponse response = mock(TemplateDetailResponse.class);
            when(templateMapper.toTemplateDetailResponse(template)).thenReturn(response);

            assertThat(service.getTemplateById(TEMPLATE_ID)).isSameAs(response);
        }
    }

    @Nested
    @DisplayName("createTemplate")
    class Create {

        @Test
        @DisplayName("builds and persists a template owned by the resolved account")
        void buildsAndSaves() {
            Account author = account(AUTHOR_ID);

            service.createTemplate(new TemplateCreateRequest("NPC", "body"), author);

            ArgumentCaptor<Template> captor = ArgumentCaptor.forClass(Template.class);
            verify(templateRepository).save(captor.capture());
            Template built = captor.getValue();
            assertThat(built.getName()).isEqualTo("NPC");
            assertThat(built.getContent()).isEqualTo("body");
            assertThat(built.getAuthor()).isSameAs(author);
        }
    }

    @Nested
    @DisplayName("updateTemplateName")
    class Update {

        @Test
        @DisplayName("throws NotFound when the template lives in another room")
        void notFound() {
            when(templateRepository.findByRoomIdAndId(ROOM_ID, TEMPLATE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateTemplateName(ROOM_ID, TEMPLATE_ID, new TemplateUpdateRequest("n"), account(AUTHOR_ID)))
                    .isInstanceOf(NotFoundException.class);
            verifyNoInteractions(roomSecurityService);
        }

        @Test
        @DisplayName("throws Forbidden when the caller is neither author nor creator")
        void forbidden() {
            Template template = Template.builder().id(TEMPLATE_ID).author(account(AUTHOR_ID)).build();
            Account stranger = account(99L);
            when(templateRepository.findByRoomIdAndId(ROOM_ID, TEMPLATE_ID)).thenReturn(Optional.of(template));
            when(roomSecurityService.isCreator(stranger)).thenReturn(false);

            assertThatThrownBy(() -> service.updateTemplateName(ROOM_ID, TEMPLATE_ID, new TemplateUpdateRequest("n"), stranger))
                    .isInstanceOf(ForbiddenException.class);
            verify(templateRepository, never()).save(any());
        }

        @Test
        @DisplayName("applies the DTO onto the entity and saves for the author")
        void appliesAndSaves() {
            Template template = Template.builder().id(TEMPLATE_ID).author(account(AUTHOR_ID)).build();
            when(templateRepository.findByRoomIdAndId(ROOM_ID, TEMPLATE_ID)).thenReturn(Optional.of(template));
            TemplateUpdateRequest request = new TemplateUpdateRequest("renamed");

            service.updateTemplateName(ROOM_ID, TEMPLATE_ID, request, account(AUTHOR_ID));

            verify(templateMapper).updateTemplateFromRequest(request, template);
            verify(templateRepository).save(template);
        }

        @Test
        @DisplayName("room creator may rename a template they did not author")
        void creatorAppliesAndSaves() {
            Template template = Template.builder().id(TEMPLATE_ID).author(account(AUTHOR_ID)).build();
            Account creator = account(99L);
            when(templateRepository.findByRoomIdAndId(ROOM_ID, TEMPLATE_ID)).thenReturn(Optional.of(template));
            when(roomSecurityService.isCreator(creator)).thenReturn(true);
            TemplateUpdateRequest request = new TemplateUpdateRequest("renamed");

            service.updateTemplateName(ROOM_ID, TEMPLATE_ID, request, creator);

            verify(templateMapper).updateTemplateFromRequest(request, template);
            verify(templateRepository).save(template);
        }
    }

    @Nested
    @DisplayName("deleteTemplate")
    class Delete {

        @Test
        @DisplayName("throws NotFound when nothing exists in the room")
        void notFound() {
            when(templateRepository.findByRoomIdAndId(ROOM_ID, TEMPLATE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteTemplate(ROOM_ID, TEMPLATE_ID, account(AUTHOR_ID)))
                    .isInstanceOf(NotFoundException.class);
            verify(templateRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws Forbidden when the caller is neither author nor creator")
        void forbidden() {
            Template template = Template.builder().id(TEMPLATE_ID).author(account(AUTHOR_ID)).build();
            Account stranger = account(99L);
            when(templateRepository.findByRoomIdAndId(ROOM_ID, TEMPLATE_ID)).thenReturn(Optional.of(template));
            when(roomSecurityService.isCreator(stranger)).thenReturn(false);

            assertThatThrownBy(() -> service.deleteTemplate(ROOM_ID, TEMPLATE_ID, stranger))
                    .isInstanceOf(ForbiddenException.class);
            verify(templateRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deletes when the author asks")
        void authorDeletes() {
            Template template = Template.builder().id(TEMPLATE_ID).author(account(AUTHOR_ID)).build();
            when(templateRepository.findByRoomIdAndId(ROOM_ID, TEMPLATE_ID)).thenReturn(Optional.of(template));

            service.deleteTemplate(ROOM_ID, TEMPLATE_ID, account(AUTHOR_ID));

            verify(templateRepository).delete(template);
        }

        @Test
        @DisplayName("deletes when the room creator asks")
        void creatorDeletes() {
            Template template = Template.builder().id(TEMPLATE_ID).author(account(AUTHOR_ID)).build();
            Account creator = account(99L);
            when(templateRepository.findByRoomIdAndId(ROOM_ID, TEMPLATE_ID)).thenReturn(Optional.of(template));
            when(roomSecurityService.isCreator(creator)).thenReturn(true);

            service.deleteTemplate(ROOM_ID, TEMPLATE_ID, creator);

            verify(templateRepository).delete(template);
        }
    }
}
