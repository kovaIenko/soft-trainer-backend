package com.backend.softtrainer.dtos;

import com.backend.softtrainer.dtos.client.UserMessageDto;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SpecificMessageResponseDto(@JsonProperty("chat_id") Long chatId,
                                         @JsonProperty("skill_id") Long skillId,
                                         boolean success,
                                         @JsonProperty("error_message") String errorMessage,
                                         UserMessageDto message) {
}
