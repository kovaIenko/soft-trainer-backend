package com.backend.softtrainer.dtos.aiagent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for initializing AI-generated simulation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInitializeSimulationRequestDto {
    
    @JsonProperty("simulation_id")
    private String simulationId;
    
    @JsonProperty("chat_id")
    private String chatId;
    
    @JsonProperty("simulation_context")
    private AiSimulationContextDto simulationContext;
    
    @JsonProperty("organization_context")
    private AiAgentOrganizationDto organizationContext;
    
    @JsonProperty("user_context")
    private Map<String, Object> userContext;
    
    @JsonProperty("initial_hyper_parameters")
    private Map<String, Object> initialHyperParameters;
} 