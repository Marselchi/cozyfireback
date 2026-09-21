package com.cozyfireplace.server.polls.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PollResponse {
    String source;
    String content;
    String profileName;
    String profileEmail;
}
