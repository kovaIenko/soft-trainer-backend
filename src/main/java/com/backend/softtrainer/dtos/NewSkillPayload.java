package com.backend.softtrainer.dtos;

import com.backend.softtrainer.entities.enums.BehaviorType;
import com.backend.softtrainer.entities.enums.SkillType;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class NewSkillPayload {

    @NotEmpty
    private String name;

    @NotEmpty
    private String description;

    @NotNull
    private SkillType type;

    @NotNull
    private BehaviorType behavior;

    private Integer simulationCount;

    private List<MaterialDto> materials;

    @Data
    public static class MaterialDto {
        private String fileName;
        private String tag;
        private String fileContent; // base64 encoded
    }
} 