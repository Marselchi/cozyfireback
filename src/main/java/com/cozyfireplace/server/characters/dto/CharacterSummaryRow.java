package com.cozyfireplace.server.characters.dto;

public interface CharacterSummaryRow {
    Long getId();
    String getName();
    String getDescription();
    String getStatus();
    String getContent();
    Long getAccountId();
    String getAccountName();
    boolean isCreatedByRoomCreator();
}
