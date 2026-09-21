package com.cozyfireplace.server.lore.dto;

public interface LoreSummaryRow {
    Long getId();
    String getTitle();
    String getDescription();
    String getDate();
    String getContent();
    Long getAccountId();
    String getAccountName();
    boolean isCreatedByRoomCreator();
    boolean isNonPublic();
    Boolean getViewed();
    Long getViewCount();
}
