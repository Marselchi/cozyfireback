package com.cozyfireplace.server.templates;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import com.cozyfireplace.server.templates.dto.*;
import com.cozyfireplace.server.util.exception.ForbiddenException;
import com.cozyfireplace.server.util.exception.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final RoomSecurityService roomSecurityService;
    private final TemplateMapper templateMapper;

    public Page<TemplateResponse> getAllTemplates(String search, Pageable pageable) {
        Page<Template> templates;
        if (search != null && !search.isBlank()) {
            templates = templateRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            templates = templateRepository.findAll(pageable);
        }
        return templates.map(templateMapper::toTemplateResponse);
    }

    public TemplateDetailResponse getTemplateById(Long templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template", templateId));
        return templateMapper.toTemplateDetailResponse(template);
    }

    /**
     * Создание шаблона. Комната выводится из аккаунта: {@code @CurrentAccount} уже
     * разрешён строго в комнате из пути, поэтому шаблон физически не может «уехать» в другую комнату.
     */
    public void createTemplate(TemplateCreateRequest request, Account account) {
        Template template = Template.builder()
                .name(request.name())
                .content(request.content())
                .author(account)
                .build();
        templateRepository.save(template);
    }

    public void updateTemplateName(Long roomId, Long templateId, TemplateUpdateRequest request, Account account) {
        Template template = findInRoom(roomId, templateId);
        checkEditPermission(template, account, "update");
        templateMapper.updateTemplateFromRequest(request, template);
        templateRepository.save(template);
    }

    public void deleteTemplate(Long roomId, Long templateId, Account account) {
        Template template = findInRoom(roomId, templateId);
        checkEditPermission(template, account, "delete");
        templateRepository.delete(template);
    }

    private Template findInRoom(Long roomId, Long templateId) {
        return templateRepository.findByRoomIdAndId(roomId, templateId)
                .orElseThrow(() -> new NotFoundException("Template", templateId));
    }

    /**
     * Комната уже проверена запросом ({@code findByRoomIdAndId}), а аккаунт гарантирован
     * {@code @CurrentAccount}, поэтому остаётся проверить только авторство либо права создателя комнаты.
     */
    private void checkEditPermission(Template template, Account account, String action) {
        boolean isCreator = roomSecurityService.isCreator(account);
        boolean isAuthor = template.getAuthor() != null && Objects.equals(template.getAuthor().getId(), account.getId());
        if (!isCreator && !isAuthor) {
            throw new ForbiddenException("Only author or room creator can " + action + " template");
        }
    }
}
