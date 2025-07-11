package com.backend.softtrainer.dtos.aiagent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for AI message generation response
 * Contains generated messages and updated state
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMessageGenerationResponseDto {
    
    @JsonProperty("messages")
    private List<AiGeneratedMessageDto> messages;
    
    @JsonProperty("updated_hyper_parameters")
    private Map<String, Object> updatedHyperParameters;
    
    @JsonProperty("conversation_ended")
    private Boolean conversationEnded;
    
    @JsonProperty("end_reason")
    private String endReason;
    
    @JsonProperty("generation_metadata")
    private Map<String, Object> generationMetadata;
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("error_message")
    private String errorMessage;
} 