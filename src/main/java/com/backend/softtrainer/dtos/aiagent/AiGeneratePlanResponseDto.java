package com.backend.softtrainer.dtos.aiagent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGeneratePlanResponseDto {
    private List<AiSimulationDto> simulations;
    
    @JsonProperty("plan_summary")
    private String planSummary;
    
    @JsonProperty("estimated_total_duration")
    private Integer estimatedTotalDuration;
    
    @JsonProperty("difficulty_progression")
    private List<String> difficultyProgression;
    
    private Boolean success;
    
    @JsonProperty("generation_metadata")
    private Map<String, Object> generationMetadata;
} 