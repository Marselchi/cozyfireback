package com.cozyfireplace.server.templates;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.templates.dto.*;
import com.cozyfireplace.server.util.exception.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final AccountRepository accountRepository;
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

    public void createTemplate(TemplateCreateRequest request, Long authorId) {
        Account author = accountRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("Account", authorId));

        Template template = Template.builder()
                .name(request.name())
                .content(request.content())
                .author(author)
                .build();
        templateRepository.save(template);
    }

    public void updateTemplateName(Long templateId, TemplateUpdateRequest request) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template", templateId));
        templateMapper.updateTemplateFromRequest(request, template);
        templateRepository.save(template);
    }

    public void deleteTemplate(Long templateId) {
        if (!templateRepository.existsById(templateId)) {
            throw new NotFoundException("Template", templateId);
        }
        templateRepository.deleteById(templateId);
    }
}
