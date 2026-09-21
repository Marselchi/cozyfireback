package com.cozyfireplace.server.lore.blocks.dto;

import com.cozyfireplace.server.lore.blocks.AccessRecord;

public interface AccessRecordWithAccount {
    AccessRecord getRecord();

    Long getUserId();

    String getUserName();
}
