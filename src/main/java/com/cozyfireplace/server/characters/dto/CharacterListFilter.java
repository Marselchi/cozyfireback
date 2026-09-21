package com.cozyfireplace.server.characters.dto;

import java.util.Set;

public record CharacterListFilter(
        Boolean createdByRoomCreator,
        Set<Long> roleIds,
        String name
) {}
