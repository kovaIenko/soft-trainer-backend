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
public class AiSimulationDto {
    private String name;
    private String description;
    private List<String> variables;
    private String difficulty;
    private Integer estimatedDuration;
} 