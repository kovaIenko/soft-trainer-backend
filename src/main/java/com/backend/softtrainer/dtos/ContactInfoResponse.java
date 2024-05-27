package com.backend.softtrainer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ContactInfoResponse(Boolean success, @JsonProperty("error_message") String errorMessage) {
}
