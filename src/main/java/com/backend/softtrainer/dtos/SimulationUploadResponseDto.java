package com.backend.softtrainer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SimulationUploadResponseDto(String name, boolean success, @JsonProperty("error_message") String errorMessage) {
}
