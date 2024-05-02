package com.backend.softtrainer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ChatsResponseDto(List<String> names, boolean success, @JsonProperty("error_message") String errorMessage) {
}

