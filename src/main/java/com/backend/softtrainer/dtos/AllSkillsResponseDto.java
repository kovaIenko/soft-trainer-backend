package com.backend.softtrainer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public record AllSkillsResponseDto(Set<SkillResponseDto> names, boolean success,
                                   @JsonProperty("error_message") String errorMessage) {
}
