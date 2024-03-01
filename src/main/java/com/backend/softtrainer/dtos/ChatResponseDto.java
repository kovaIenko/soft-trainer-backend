package com.backend.softtrainer.dtos;


import com.backend.softtrainer.entities.messages.Message;

import java.util.List;

//todo messages are here is so stupid approach but it is temporary
public record ChatResponseDto(String chatId, boolean success, String errorMessage, List<Message> messages) {
}
