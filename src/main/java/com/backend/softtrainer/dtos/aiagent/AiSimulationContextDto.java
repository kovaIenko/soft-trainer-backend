package com.backend.softtrainer.dtos.aiagent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing simulation context for AI generation
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
    
    @JsonProperty("skill_name")
    private String skillName;
    
    @JsonProperty("skill_description")
    private String skillDescription;
    
    @JsonProperty("complexity")
    private String complexity;
    
    @JsonProperty("conversation_turn")
    private Integer conversationTurn;
    
    @JsonProperty("hearts_remaining")
    private Double heartsRemaining;
    
    @JsonProperty("characters")
    private String characters;
    
    @JsonProperty("learning_objectives")
    private String learningObjectives;
} 