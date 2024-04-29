package com.backend.softtrainer.dtos;

import java.util.Set;

public record AllSkillsResponseDto(Set<SkillResponseDto> names, boolean success, String errorMessage) {
}
