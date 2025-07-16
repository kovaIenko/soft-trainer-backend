package com.backend.softtrainer.dtos.aiagent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSkillMaterialDto {
    
    @NotBlank(message = "Material filename cannot be empty")
    @Size(max = 255, message = "Material filename too long")
    private String filename;
    
    @NotBlank(message = "Material content cannot be empty")
    @Size(max = 50000, message = "Material content too large (max 50KB)")
    private String content;
} 