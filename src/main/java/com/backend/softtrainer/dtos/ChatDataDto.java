package com.backend.softtrainer.dtos;

import com.backend.softtrainer.entities.messages.Message;

import java.util.List;

public record ChatDataDto(List<Message> messages, ChatParams params) {
}
