package com.backend.softtrainer.dtos;

import com.backend.softtrainer.dtos.client.UserMessageDto;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ChatResponseDto(@JsonProperty("chat_id") Long chatId,
                              @JsonProperty("skill_id") Long skillId,
                              boolean success,
                              @JsonProperty("error_message") String errorMessage,
                              List<UserMessageDto> messages) {
}
