package com.backend.softtrainer.dtos;

import com.backend.softtrainer.entities.enums.BehaviorType;
import com.backend.softtrainer.entities.enums.SkillType;
import com.backend.softtrainer.entities.enums.SkillGenerationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SkillDetailDto {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("avatar")
    private String avatar;
    @JsonProperty("description")
    private String description;
    @JsonProperty("type")
    private SkillType type;
    @JsonProperty("behavior")
    private BehaviorType behavior;
    @JsonProperty("simulation_count")
    private Integer simulationCount;
    @JsonProperty("generation_status")
    private SkillGenerationStatus generationStatus;
    @JsonProperty("simulations")
    private List<SimulationNodesDto> simulations;
    @JsonProperty("materials")
    private List<MaterialDetailDto> materials;

    @Data
    @Builder
    public static class MaterialDetailDto {
        @JsonProperty("file_name")
        private String fileName;
        @JsonProperty("tag")
        private String tag;
    }
}
