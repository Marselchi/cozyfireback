package com.cozyfireplace.server.answers.dto;

import java.time.Instant;

public interface AnswerPathProjection {

    Long getId();
    String getContent();
    Instant getCreatedAt();
    Instant getUpdatedAt();
    Long getParentId();

    Long getAuthorId();
    String getUsername();

    Integer getReplyCount();
    Boolean getIsAdmin();
}
