package com.backend.softtrainer.dtos;

import java.util.List;
import java.util.Set;

public record ChatDto(Set<MessageDto> messages) {
}
