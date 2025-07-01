package com.backend.softtrainer.dtos.aiagent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAgentSkillDto {
    private String name;
    private String description;
    private List<Object> materials; // Empty list for now
    private String targetAudience;
    private String complexityLevel;
    private Integer expectedCountSimulations;
} 