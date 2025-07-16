package com.backend.softtrainer.services;

import com.backend.softtrainer.configs.AiAgentErrorHandler;
import com.backend.softtrainer.dtos.aiagent.AiAgentOrganizationDto;
import com.backend.softtrainer.dtos.aiagent.AiAgentSkillDto;
import com.backend.softtrainer.dtos.aiagent.AiGeneratePlanRequestDto;
import com.backend.softtrainer.dtos.aiagent.AiGeneratePlanResponseDto;
import com.backend.softtrainer.dtos.aiagent.AiGeneratedMessageDto;
import com.backend.softtrainer.dtos.aiagent.AiInitializeSimulationRequestDto;
import com.backend.softtrainer.dtos.aiagent.AiMessageGenerationRequestDto;
import com.backend.softtrainer.dtos.aiagent.AiMessageGenerationResponseDto;
import com.backend.softtrainer.dtos.aiagent.AiSkillMaterialDto;
import com.backend.softtrainer.entities.Organization;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.services.validation.AiAgentResponseValidator;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAgentService {

    @Qualifier("aiAgentRestTemplate")
    private final RestTemplate restTemplate;

    private final AiAgentResponseValidator responseValidator;

    @Value("${app.ai-agent.base-url:http://16.171.20.54:8000}")
    private String aiAgentBaseUrl;

    @Value("${app.ai-agent.enabled:true}")
    private boolean aiAgentEnabled;

    @Value("${app.ai-agent.fallback.enabled:false}")
    private boolean fallbackEnabled;

    @Value("${app.ai-agent.timeout.read:3000}")
    private int readTimeoutMs;

    @Value("${app.ai-agent.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.ai-agent.retry.backoff-ms:200}")
    private int retryBackoffMs;

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
            AiGeneratePlanResponseDto response = callAiAgentWithRetry(request, "/generate-plan");

            log.info("Successfully generated AI plan for skill: {} with {} simulations",
                    skill.getName(), response.getSimulations().size());

            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("Failed to generate AI plan for skill: {}", skill.getName(), e);
            if (fallbackEnabled) {
                log.warn("Using fallback response due to AI agent failure");
                return CompletableFuture.completedFuture(createFallbackResponse());
            } else {
                throw new RuntimeException("AI plan generation failed and fallback is disabled", e);
            }
        }
    }

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

        // Convert skill materials to AI agent format
        List<AiSkillMaterialDto> skillMaterials = convertSkillMaterials(skill);

        return AiGeneratePlanRequestDto.builder()
                .organization(orgDto)
                .skill(skillDto)
                .skillMaterials(skillMaterials)
                .build();
    }

    private AiGeneratePlanResponseDto callAiAgentWithRetry(AiGeneratePlanRequestDto request, String endpoint) {
        String url = aiAgentBaseUrl + endpoint;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AiGeneratePlanRequestDto> entity = new HttpEntity<>(request, headers);

        // --- ADDED LOGGING ---
        log.trace("[AI_AGENT_REQUEST] DTO: {}", request);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            String requestJson = mapper.writeValueAsString(request);
            log.debug("[AI_AGENT_REQUEST] JSON for /generate-plan: {}", requestJson);
        } catch (Exception e) {
            log.warn("[AI_AGENT_REQUEST] Could not serialize request for logging", e);
        }
        // Log all headers
        log.debug("[AI_AGENT_REQUEST] HTTP headers for /generate-plan: {}", headers);
        // --- END LOGGING ---

        // Retry logic with exponential backoff
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                long startTime = System.currentTimeMillis();

                ResponseEntity<AiGeneratePlanResponseDto> response = restTemplate.postForEntity(
                        url, entity, AiGeneratePlanResponseDto.class);

                long responseTime = System.currentTimeMillis() - startTime;
                log.info("AI Agent response time: {}ms (attempt {}/{})", responseTime, attempt, maxRetryAttempts);

                if (response.getBody() == null) {
                    throw new RuntimeException("AI Agent returned null response");
                }
                // --- ADDED LOGGING ---
                log.trace("[AI_AGENT_RESPONSE] DTO: {}", response.getBody());
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                    String responseJson = mapper.writeValueAsString(response.getBody());
                    log.debug("[AI_AGENT_RESPONSE] JSON for /generate-plan: {}", responseJson);
                } catch (Exception e) {
                    log.warn("[AI_AGENT_RESPONSE] Could not serialize response for logging", e);
                }
                // --- END LOGGING ---
                return response.getBody();

            } catch (Exception e) {
                lastException = e;
                log.warn("AI Agent call failed (attempt {}/{}): {}", attempt, maxRetryAttempts, e.getMessage());

                if (attempt < maxRetryAttempts) {
                    try {
                        Thread.sleep(retryBackoffMs * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }

        throw new RuntimeException("AI Agent call failed after " + maxRetryAttempts + " attempts", lastException);
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
     * Convert skill materials to AI agent format with content truncation
     */
    private List<AiSkillMaterialDto> convertSkillMaterials(Skill skill) {
        if (skill.getMaterials() == null || skill.getMaterials().isEmpty()) {
            return Collections.emptyList();
        }

        return skill.getMaterials().stream()
                .map(material -> {
                    String content = material.getFileContent() != null ?
                        new String(material.getFileContent()) :
                        "Material content not available";

                    // Truncate content if too large (>20KB)
                    if (content.length() > 20000) {
                        log.warn("Truncating material content from {} to 20000 chars for material: {}",
                                content.length(), material.getFileName());
                        content = content.substring(0, 20000) + "...";
                    }

                    return AiSkillMaterialDto.builder()
                            .filename(material.getFileName())
                            .content(content)
                            .build();
                })
                .collect(Collectors.toList());
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

            String url = aiAgentBaseUrl + "/send-message";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AiMessageGenerationRequestDto> entity = new HttpEntity<>(request, headers);

            log.debug("Calling AI Agent for message generation (send-message): {} with chat_id: {}",
                    url, request.getChatId());

            // Use retry logic for message generation
            ResponseEntity<AiMessageGenerationResponseDto> response = callMessageAgentWithRetry(entity, url);

            if (response.getBody() == null) {
                throw new RuntimeException("AI Agent returned null response for message generation");
            }
            log.info("AI Agent returned {} for chat: {}",
                    response.getBody(), request.getChatId());
            // ✅ Validate response before returning
            AiMessageGenerationResponseDto validatedResponse = validateAndSanitizeResponse(response.getBody());

            // --- ADDED LOGGING ---
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                String responseJson = mapper.writeValueAsString(validatedResponse);
                log.info("[AI_AGENT_RESPONSE] Full validated AI response for chat {}: {}", request.getChatId(), responseJson);
            } catch (Exception e) {
                log.warn("[AI_AGENT_RESPONSE] Could not serialize AI response for logging", e);
            }
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
            if (fallbackEnabled) {
                log.warn("Using fallback response due to AI agent failure");
                return createFallbackMessageResponse();
            } else {
                throw new RuntimeException("Message generation failed and fallback is disabled", e);
            }
        }
    }

    /**
     * 🎬 Initialize AI-generated simulation using /create-chat endpoint
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
            String url = aiAgentBaseUrl + "/create-chat";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AiInitializeSimulationRequestDto> entity = new HttpEntity<>(request, headers);

            log.debug("Calling AI Agent for simulation initialization (create-chat): {} with simulation_id: {}",
                    url, request.getSimulationId());

            // Use retry logic for initialization
            ResponseEntity<AiMessageGenerationResponseDto> response = callMessageAgentWithRetry(entity, url);

            if (response.getBody() == null) {
                throw new RuntimeException("AI Agent returned null response for simulation initialization");
            }

            // ✅ Validate response before returning
            AiMessageGenerationResponseDto validatedResponse = validateAndSanitizeResponse(response.getBody());

            // --- ADDED LOGGING ---
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                String responseJson = mapper.writeValueAsString(validatedResponse);
                log.info("[AI_AGENT_RESPONSE] Full validated AI response for simulation {}: {}", request.getSimulationId(), responseJson);
            } catch (Exception e) {
                log.warn("[AI_AGENT_RESPONSE] Could not serialize AI response for logging", e);
            }
            log.info("Successfully initialized simulation with {} messages for simulation: {}",
                    validatedResponse.getMessages().size(), request.getSimulationId());

            return validatedResponse;

        } catch (Exception e) {
            log.error("Failed to initialize simulation: {}", request.getSimulationId(), e);
            if (fallbackEnabled) {
                log.warn("Using fallback response due to AI agent failure");
                return createFallbackMessageResponse();
            } else {
                throw new RuntimeException("Simulation initialization failed and fallback is disabled", e);
            }
        }
    }

    /**
     * Retry logic for message agent calls
     */
    private ResponseEntity<AiMessageGenerationResponseDto> callMessageAgentWithRetry(
            HttpEntity<?> entity, String url) {

        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                // Log all headers before sending
                log.debug("[AI_AGENT_REQUEST] HTTP headers for {}: {}", url, entity.getHeaders());
                long startTime = System.currentTimeMillis();

                ResponseEntity<AiMessageGenerationResponseDto> response = restTemplate.postForEntity(
                        url, entity, AiMessageGenerationResponseDto.class);

                long responseTime = System.currentTimeMillis() - startTime;
                log.info("AI Agent message response time: {}ms (attempt {}/{})", responseTime, attempt, maxRetryAttempts);

                return response;

            } catch (Exception e) {
                lastException = e;
                log.warn("AI Agent message call failed (attempt {}/{}): {}", attempt, maxRetryAttempts, e.getMessage());

                if (attempt < maxRetryAttempts) {
                    try {
                        Thread.sleep(retryBackoffMs * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }

        throw new RuntimeException("AI Agent message call failed after " + maxRetryAttempts + " attempts", lastException);
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

        // Log raw response BEFORE validation for debugging (in case validation fails)
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String rawResponseJson = mapper.writeValueAsString(response);
            log.info("[AI_AGENT_RESPONSE] Raw AI response (before validation): {}", rawResponseJson);
        } catch (Exception e) {
            log.warn("[AI_AGENT_RESPONSE] Could not serialize raw AI response for logging", e);
        }

        // CRITICAL FIX: Sanitize response to fix requires_response field mismatches
        sanitizeAiResponse(response);

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
     * 🔧 CRITICAL FIX: Sanitize AI response to fix requires_response field mismatches
     * 
     * The AI agent sometimes incorrectly sets requires_response=true for Text messages,
     * which causes validation failures and triggers fallback responses.
     * This method automatically corrects these issues based on message type.
     */
    private void sanitizeAiResponse(AiMessageGenerationResponseDto response) {
        if (response == null || response.getMessages() == null) {
            return;
        }

        int fixedCount = 0;
        for (AiGeneratedMessageDto message : response.getMessages()) {
            String messageType = message.getMessageType();
            Boolean requiresResponse = message.getRequiresResponse();

            // Fix Text and Hint messages (should NOT require response)
            if (("Text".equals(messageType) || "Hint".equals(messageType))) {
                if (requiresResponse != null && requiresResponse) {
                    log.info("🔧 AUTO-FIXING: Setting requires_response=false for {} message", messageType);
                    message.setRequiresResponse(false);
                    fixedCount++;
                }
            }
            // Fix Choice questions (should require response)
            else if (("SingleChoiceQuestion".equals(messageType) || 
                     "MultipleChoiceQuestion".equals(messageType) || 
                     "EnterTextQuestion".equals(messageType))) {
                if (requiresResponse == null || !requiresResponse) {
                    log.info("🔧 AUTO-FIXING: Setting requires_response=true for {} message", messageType);
                    message.setRequiresResponse(true);
                    fixedCount++;
                }
            }
        }

        if (fixedCount > 0) {
            log.info("✅ SANITIZATION: Fixed {} requires_response field(s) in AI response", fixedCount);
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
