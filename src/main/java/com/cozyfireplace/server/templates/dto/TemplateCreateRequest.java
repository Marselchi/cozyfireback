package com.cozyfireplace.server.templates.dto;

/**
 * DTO для создания шаблона.
 */
public record TemplateCreateRequest(String name, String content) {
}
