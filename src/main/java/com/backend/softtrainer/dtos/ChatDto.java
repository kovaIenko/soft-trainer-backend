package com.backend.softtrainer.dtos;

import com.backend.softtrainer.entities.messages.Message;

import java.util.Set;

public record ChatDto(Set<Message> messages) {
}
