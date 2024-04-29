package com.backend.softtrainer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SkillRequestDto(@JsonProperty("skill_id") Long skillId, String name, String avatar) {
}
