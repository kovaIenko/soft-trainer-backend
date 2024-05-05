package com.backend.softtrainer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SignUpUserResponseDto(String email, boolean status, @JsonProperty("error_message") String errorMessage) {
}
