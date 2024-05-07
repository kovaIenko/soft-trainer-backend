package com.backend.softtrainer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public record AllSkillsResponseDto(Set<SkillResponseDto> names,
                                   Set<SkillResponseDto> skills,
                                   boolean success,
                                   @JsonProperty("error_message") String errorMessage) {
}
