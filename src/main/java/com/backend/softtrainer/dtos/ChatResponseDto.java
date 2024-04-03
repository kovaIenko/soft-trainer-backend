package com.backend.softtrainer.dtos;

import com.backend.softtrainer.dtos.client.UserMessageDto;

import java.util.List;

public record ChatResponseDto(Long chatId, boolean success, String errorMessage, List<UserMessageDto> messages) {
}
