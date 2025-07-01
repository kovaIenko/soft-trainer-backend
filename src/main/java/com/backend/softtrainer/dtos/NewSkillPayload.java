package com.backend.softtrainer.dtos;

import com.backend.softtrainer.entities.enums.BehaviorType;
import com.backend.softtrainer.entities.enums.SkillType;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.backend.softtrainer.utils.WordCount;
import java.util.List;

@Data
public class NewSkillPayload {

    @NotEmpty
    private String name;

    @NotEmpty
    @WordCount(max = 3000, message = "Description cannot exceed 3000 words")
    @Size(max = 20000, message = "Description text is too long")
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