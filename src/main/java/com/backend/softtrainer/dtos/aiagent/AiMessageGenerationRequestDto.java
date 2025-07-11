package com.backend.softtrainer.dtos.aiagent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for requesting real-time AI message generation
 * Contains chat context and user input for the AI agent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMessageGenerationRequestDto {
    
    @JsonProperty("simulation_id")
    private String simulationId;
    
    @JsonProperty("chat_id")
    private String chatId;
    
    @JsonProperty("chat_history")
    private List<AiChatMessageDto> chatHistory;
    
    @JsonProperty("user_message")
    private AiUserMessageDto userMessage;
    
    @JsonProperty("simulation_context")
    private AiSimulationContextDto simulationContext;
    
    @JsonProperty("hyper_parameters")
    private Map<String, Object> hyperParameters;
    
    @JsonProperty("organization_context")
    private AiAgentOrganizationDto organizationContext;
} 