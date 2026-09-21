package com.cozyfireplace.server.templates.dto;

/**
 * DTO для ответа в списке шаблонов (без контента).
 */
public record TemplateResponse(Long id, String name) {
}
