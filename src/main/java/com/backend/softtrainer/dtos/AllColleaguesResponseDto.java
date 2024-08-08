package com.backend.softtrainer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public record AllColleaguesResponseDto(List<UserDto> users,
                                       boolean success,
                                       @JsonProperty("error_message") String errorMessage) {
}

