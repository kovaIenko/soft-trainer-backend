package com.backend.softtrainer.dtos;

import com.backend.softtrainer.entities.enums.BehaviorType;
import com.backend.softtrainer.entities.enums.SkillType;
import com.backend.softtrainer.entities.enums.SkillGenerationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillListDto {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("avatar")
    private String avatar;
    @JsonProperty("type")
    private SkillType type;
    @JsonProperty("behavior")
    private BehaviorType behavior;
    @JsonProperty("simulation_count")
    private Integer simulationCount;
    @JsonProperty("generation_status")
    private SkillGenerationStatus generationStatus;
    @JsonProperty("is_hidden")
    private boolean isHidden;
    @JsonProperty("is_protected")
    private boolean isProtected;
} 