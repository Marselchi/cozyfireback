package com.cozyfireplace.server.util.validation;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class RoomValidationErrors {
    String message;
    List<String> nameErrors;
    List<String> descriptionErrors;
    List<String> otherErrors;
}
