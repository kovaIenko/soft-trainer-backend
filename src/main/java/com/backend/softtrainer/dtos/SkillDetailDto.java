package com.backend.softtrainer.dtos;

import com.backend.softtrainer.entities.enums.BehaviorType;
import com.backend.softtrainer.entities.enums.SkillType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SkillDetailDto {
    private Long id;
    private String name;
    private String description;
    private SkillType type;
    private BehaviorType behavior;
    private Integer simulationCount;
    private List<SimulationNodesDto> simulations;
    private List<MaterialDetailDto> materials;

    @Data
    @Builder
    public static class MaterialDetailDto {
        private String fileName;
        private String tag;
    }
}
