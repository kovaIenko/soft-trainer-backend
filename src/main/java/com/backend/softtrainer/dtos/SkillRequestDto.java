package com.backend.softtrainer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SkillRequestDto(@JsonProperty("skill_id") Long skillId, Long id, String name, String avatar) {
}
