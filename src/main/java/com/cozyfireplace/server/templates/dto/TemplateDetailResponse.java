package com.cozyfireplace.server.templates.dto;

/**
 * DTO для полного ответа шаблона (с контентом).
 */
public record TemplateDetailResponse(Long id, String name, String content) {
}
