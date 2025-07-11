package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.aiagent.*;
import com.backend.softtrainer.entities.Organization;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.enums.SkillGenerationStatus;
import com.backend.softtrainer.services.validation.AiAgentResponseValidator;
import com.backend.softtrainer.configs.AiAgentErrorHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAgentService {

    @Qualifier("aiAgentRestTemplate")
    private final RestTemplate restTemplate;
    
    private final AiAgentResponseValidator responseValidator;
    // private final HybridAiProcessingService hybridProcessingService;  // Will inject when ready
    
    @Value("${app.ai-agent.base-url:http://16.171.20.54:8000}")
    private String aiAgentBaseUrl;
    
    @Value("${app.ai-agent.enabled:true}")
    private boolean aiAgentEnabled;

    @Async("aiAgentTaskExecutor")
    public CompletableFuture<AiGeneratePlanResponseDto> generatePlanAsync(Skill skill, Organization organization) {
        log.info("Starting async AI plan generation for skill: {} in organization: {}", 
                skill.getName(), organization.getName());
        
        if (!aiAgentEnabled) {
            log.warn("AI Agent is disabled, skipping plan generation");
            return CompletableFuture.completedFuture(createFallbackResponse());
        }
        
        try {
            AiGeneratePlanRequestDto request = buildRequest(skill, organization);
            AiGeneratePlanResponseDto response = callAiAgent(request);
            
            log.info("Successfully generated AI plan for skill: {} with {} simulations", 
                    skill.getName(), response.getSimulations().size());
            
            return CompletableFuture.completedFuture(response);
            
        } catch (Exception e) {
            log.error("Failed to generate AI plan for skill: {}", skill.getName(), e);
            return CompletableFuture.completedFuture(createFallbackResponse());
        }
    }

    /**
     * 🚀 Enhanced AI Plan Generation with Smart Processing
     * Uses hybrid processing to choose between legacy and enhanced systems
     * TODO: Re-enable when HybridAiProcessingService is ready
     */
    /*
    @Async("aiAgentTaskExecutor")
    public CompletableFuture<Void> generateAndProcessPlanAsync(Skill skill, Organization organization) {
        log.info("🚀 Starting enhanced AI plan generation and processing for skill: {}", skill.getName());
        
        if (!aiAgentEnabled) {
            log.warn("AI Agent is disabled, skipping plan generation");
            return CompletableFuture.completedFuture(null);
        }
        
        try {
            // Generate plan using AI agent
            AiGeneratePlanRequestDto request = buildRequest(skill, organization);
            AiGeneratePlanResponseDto response = callAiAgent(request);
            
            log.info("✅ Generated AI plan for skill: {} with {} simulations", 
                    skill.getName(), response.getSimulations().size());
            
            // Process plan using hybrid system (enhanced or legacy)
            return hybridProcessingService.processAiPlanWithSmartRouting(skill.getId(), response);
            
        } catch (Exception e) {
            log.error("❌ Failed to generate and process AI plan for skill: {}", skill.getName(), e);
            return CompletableFuture.completedFuture(null);
        }
    }
    */

    private AiGeneratePlanRequestDto buildRequest(Skill skill, Organization organization) {
        AiAgentOrganizationDto orgDto = AiAgentOrganizationDto.builder()
                .name(organization.getName())
                .industry(determineIndustry(organization))
                .size(determineOrganizationSize(organization))
                .localization("en")
                .build();

        AiAgentSkillDto skillDto = AiAgentSkillDto.builder()
                .name(skill.getName())
                .description(skill.getDescription())
                .materials(Collections.emptyList()) // TODO: Convert materials later
                .targetAudience("Employees")
                .complexityLevel("mixed")
                .expectedCountSimulations(skill.getSimulationCount() != null ? skill.getSimulationCount() : 3)
                .build();

        return AiGeneratePlanRequestDto.builder()
                .organization(orgDto)
                .skill(skillDto)
                .build();
    }

    private AiGeneratePlanResponseDto callAiAgent(AiGeneratePlanRequestDto request) {
        String url = aiAgentBaseUrl + "/generate-plan";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<AiGeneratePlanRequestDto> entity = new HttpEntity<>(request, headers);
        
        log.debug("Calling AI Agent at: {} with request: {}", url, request);
        
        ResponseEntity<AiGeneratePlanResponseDto> response = restTemplate.postForEntity(
                url, entity, AiGeneratePlanResponseDto.class);
        
        if (response.getBody() == null) {
            throw new RuntimeException("AI Agent returned null response");
        }
        
        return response.getBody();
    }

    private AiGeneratePlanResponseDto createFallbackResponse() {
        return AiGeneratePlanResponseDto.builder()
                .simulations(Collections.emptyList())
                .planSummary("AI generation failed, fallback response")
                .estimatedTotalDuration(0)
                .difficultyProgression(Collections.emptyList())
                .success(false)
                .generationMetadata(Collections.singletonMap("fallback", true))
                .build();
    }

    private String determineIndustry(Organization organization) {
        // TODO: Add industry field to Organization entity or derive from name
        return "Technology"; // Default for now
    }

    private String determineOrganizationSize(Organization organization) {
        // TODO: Add size field to Organization entity or derive from user count
        return "50-100 employees"; // Default for now
    }
    
    /**
     * 🤖 Generate real-time message based on chat context
     * 
     * @param request AI message generation request with chat context
     * @return AI response with generated messages
     */
    public AiMessageGenerationResponseDto generateMessage(AiMessageGenerationRequestDto request) {
        if (!aiAgentEnabled) {
            log.warn("AI Agent is disabled, returning fallback message response");
            return createFallbackMessageResponse();
        }
        
        // 🚦 Circuit breaker check
        if (AiAgentErrorHandler.isCircuitBreakerActive()) {
            log.warn("🔥 Circuit breaker is active, bypassing AI Agent for chat: {}", request.getChatId());
            return createFallbackMessageResponse();
        }
        
        try {
            // 📝 Validate request before sending
            validateAiRequest(request);
            
            String url = aiAgentBaseUrl + "/generate-message";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<AiMessageGenerationRequestDto> entity = new HttpEntity<>(request, headers);
            
            log.debug("Calling AI Agent for message generation: {} with chat_id: {}", 
                    url, request.getChatId());
            
            ResponseEntity<AiMessageGenerationResponseDto> response = restTemplate.postForEntity(
                    url, entity, AiMessageGenerationResponseDto.class);
            
            if (response.getBody() == null) {
                throw new RuntimeException("AI Agent returned null response for message generation");
            }
            
            // ✅ Validate response before returning
            AiMessageGenerationResponseDto validatedResponse = validateAndSanitizeResponse(response.getBody());
            
            // 🎯 Record success for circuit breaker
            AiAgentErrorHandler.recordSuccess();
            
            log.info("Successfully generated {} messages for chat: {}", 
                    validatedResponse.getMessages().size(), request.getChatId());
            
            return validatedResponse;
            
        } catch (AiAgentErrorHandler.AiAgentRateLimitException e) {
            log.warn("⏰ Rate limit exceeded for chat: {}", request.getChatId());
            return createFallbackMessageResponse();
        } catch (AiAgentErrorHandler.AiAgentServerException e) {
            log.error("🔥 Server error for chat: {} - using fallback", request.getChatId());
            return createFallbackMessageResponse();
        } catch (Exception e) {
            log.error("Failed to generate message for chat: {}", request.getChatId(), e);
            return createFallbackMessageResponse();
        }
    }
    
    /**
     * 🎬 Initialize AI-generated simulation
     * 
     * @param request AI initialization request
     * @return AI response with initial messages
     */
    public AiMessageGenerationResponseDto initializeSimulation(AiInitializeSimulationRequestDto request) {
        if (!aiAgentEnabled) {
            log.warn("AI Agent is disabled, returning fallback initialization response");
            return createFallbackMessageResponse();
        }
        
        try {
            String url = aiAgentBaseUrl + "/initialize-simulation";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<AiInitializeSimulationRequestDto> entity = new HttpEntity<>(request, headers);
            
            log.debug("Calling AI Agent for simulation initialization: {} with simulation_id: {}", 
                    url, request.getSimulationId());
            
            ResponseEntity<AiMessageGenerationResponseDto> response = restTemplate.postForEntity(
                    url, entity, AiMessageGenerationResponseDto.class);
            
            if (response.getBody() == null) {
                throw new RuntimeException("AI Agent returned null response for simulation initialization");
            }
            
            log.info("Successfully initialized simulation with {} messages for simulation: {}", 
                    response.getBody().getMessages().size(), request.getSimulationId());
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("Failed to initialize simulation: {}", request.getSimulationId(), e);
            return createFallbackMessageResponse();
        }
    }
    
    /**
     * 🚨 Create fallback message response when AI agent is unavailable
     */
    private AiMessageGenerationResponseDto createFallbackMessageResponse() {
        return AiMessageGenerationResponseDto.builder()
                .messages(Collections.emptyList())
                .conversationEnded(false)
                .success(false)
                .errorMessage("AI Agent is unavailable")
                .generationMetadata(Collections.singletonMap("fallback", true))
                .build();
    }
    
    /**
     * 📝 Validate AI request before sending
     */
    private void validateAiRequest(AiMessageGenerationRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("AI request cannot be null");
        }
        if (request.getChatId() == null) {
            throw new IllegalArgumentException("Chat ID cannot be null");
        }
        if (request.getChatHistory() != null && request.getChatHistory().size() > 100) {
            log.warn("⚠️ Large chat history ({} messages) for chat {}", 
                    request.getChatHistory().size(), request.getChatId());
            // Trim to last 50 messages to prevent excessive payload
            int fromIndex = Math.max(0, request.getChatHistory().size() - 50);
            request.setChatHistory(request.getChatHistory().subList(fromIndex, request.getChatHistory().size()));
        }
    }
    
    /**
     * ✅ Validate and sanitize AI response using comprehensive validator
     */
    private AiMessageGenerationResponseDto validateAndSanitizeResponse(AiMessageGenerationResponseDto response) {
        if (response == null) {
            log.error("❌ AI response is null");
            return createFallbackMessageResponse();
        }
        
        // Use comprehensive validator
        AiAgentResponseValidator.ValidationResult validationResult = responseValidator.validateResponse(response);
        
        if (!validationResult.isValid()) {
            log.error("❌ AI response validation failed: {}", validationResult.getErrorMessage());
            
            // Log detailed validation errors for debugging
            validationResult.getErrors().forEach(error -> 
                log.warn("🔍 Validation error: {}", error));
            
            // Return fallback response for invalid AI responses
            return createFallbackMessageResponse();
        }
        
        // Additional sanitization for production safety
        sanitizeResponse(response);
        
        log.debug("✅ AI response validation passed");
        return response;
    }
    
    /**
     * 🧹 Additional sanitization for production safety
     */
    private void sanitizeResponse(AiMessageGenerationResponseDto response) {
        if (response.getMessages() != null) {
            // Limit message count to prevent abuse
            if (response.getMessages().size() > 10) {
                log.warn("⚠️ AI returned {} messages, limiting to 10", response.getMessages().size());
                response.setMessages(response.getMessages().subList(0, 10));
            }
            
            // Sanitize each message
            response.getMessages().forEach(this::sanitizeMessage);
        }
        
        // Validate hyperparameters
        if (response.getUpdatedHyperParameters() != null) {
            validateHyperParameters(response.getUpdatedHyperParameters());
        }
    }
    
    /**
     * 🧹 Sanitize individual message content
     */
    private void sanitizeMessage(AiGeneratedMessageDto message) {
        if (message.getContent() != null) {
            // Limit message content length
            if (message.getContent().length() > 5000) {
                log.warn("⚠️ Truncating long message content from {} to 5000 chars", message.getContent().length());
                message.setContent(message.getContent().substring(0, 5000) + "...");
            }
            
            // Additional XSS prevention (already handled by validator but extra safety)
            String sanitized = message.getContent()
                    .replaceAll("(?i)<script[^>]*>.*?</script>", "")
                    .replaceAll("(?i)javascript:", "")
                    .replaceAll("(?i)on\\w+\\s*=", "");
            message.setContent(sanitized);
        }
        
        // Validate options list
        if (message.getOptions() != null && message.getOptions().size() > 20) {
            log.warn("⚠️ Too many options ({}), limiting to 20", message.getOptions().size());
            message.setOptions(message.getOptions().subList(0, 20));
        }
    }
    
    /**
     * 🔢 Validate hyperparameter values
     */
    private void validateHyperParameters(java.util.Map<String, Object> hyperParams) {
        hyperParams.entrySet().removeIf(entry -> {
            try {
                double value = entry.getValue() instanceof Number ? 
                    ((Number) entry.getValue()).doubleValue() : 
                    Double.parseDouble(entry.getValue().toString());
                
                if (value < 0.0 || value > 1.0) {
                    log.warn("⚠️ Invalid hyperparameter value: {}={}, removing", entry.getKey(), value);
                    return true;
                }
                return false;
            } catch (NumberFormatException e) {
                log.warn("⚠️ Non-numeric hyperparameter value: {}={}, removing", entry.getKey(), entry.getValue());
                return true;
            }
        });
    }
} 