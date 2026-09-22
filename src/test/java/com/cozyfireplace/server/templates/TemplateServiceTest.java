package com.cozyfireplace.server.templates;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.templates.dto.TemplateCreateRequest;
import com.cozyfireplace.server.templates.dto.TemplateDetailResponse;
import com.cozyfireplace.server.templates.dto.TemplateMapper;
import com.cozyfireplace.server.templates.dto.TemplateResponse;
import com.cozyfireplace.server.templates.dto.TemplateUpdateRequest;
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
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit tests for {@link TemplateService}.
 * <p>
 * The repository and MapStruct mapper are mocked. Exercises the search-vs-browse branch of
 * listing, the entity construction on create, the DTO-onto-entity update and the not-found
 * guards. JPA pagination internals are supplied via a real {@link PageImpl} so the
 * {@code Page#map} projection is genuinely exercised.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateService")
class TemplateServiceTest {

    private static final Long TEMPLATE_ID = 2L;
    private static final Long AUTHOR_ID = 42L;

    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TemplateMapper templateMapper;

    @InjectMocks
    private TemplateService service;

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
        @DisplayName("throws NotFound when the author account is missing")
        void authorNotFound() {
            when(accountRepository.findById(AUTHOR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createTemplate(new TemplateCreateRequest("n", "c"), AUTHOR_ID))
                    .isInstanceOf(NotFoundException.class);
            verify(templateRepository, never()).save(any());
        }

        @Test
        @DisplayName("builds and persists a template owned by the author")
        void buildsAndSaves() {
            Account author = mock(Account.class);
            when(accountRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(author));

            service.createTemplate(new TemplateCreateRequest("NPC", "body"), AUTHOR_ID);

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
        @DisplayName("throws NotFound when the template is missing")
        void notFound() {
            when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateTemplateName(TEMPLATE_ID, new TemplateUpdateRequest("n")))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("applies the DTO onto the entity and saves")
        void appliesAndSaves() {
            Template template = Template.builder().id(TEMPLATE_ID).build();
            when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
            TemplateUpdateRequest request = new TemplateUpdateRequest("renamed");

            service.updateTemplateName(TEMPLATE_ID, request);

            verify(templateMapper).updateTemplateFromRequest(request, template);
            verify(templateRepository).save(template);
        }
    }

    @Nested
    @DisplayName("deleteTemplate")
    class Delete {

        @Test
        @DisplayName("throws NotFound when nothing exists")
        void notFound() {
            when(templateRepository.existsById(TEMPLATE_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.deleteTemplate(TEMPLATE_ID)).isInstanceOf(NotFoundException.class);
            verify(templateRepository, never()).deleteById(TEMPLATE_ID);
        }

        @Test
        @DisplayName("deletes when present")
        void deletes() {
            when(templateRepository.existsById(TEMPLATE_ID)).thenReturn(true);

            service.deleteTemplate(TEMPLATE_ID);

            verify(templateRepository).deleteById(TEMPLATE_ID);
        }
    }
}
