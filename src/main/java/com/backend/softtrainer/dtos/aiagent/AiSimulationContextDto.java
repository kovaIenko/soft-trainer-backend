package com.backend.softtrainer.dtos.aiagent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO representing simulation context for AI generation
 * Matches the updated AI agent schema
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSimulationContextDto {
    
    @JsonProperty("simulation_name")
    private String simulationName;
    
    @JsonProperty("simulation_description")
    private String simulationDescription;
    
    @JsonProperty("learning_objectives")
    private String learningObjectives;
    
    @JsonProperty("character_info")
    private String characterInfo;
    
    @JsonProperty("user_context")
    private Map<String, Object> userContext;
} 