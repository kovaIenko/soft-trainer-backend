package com.backend.softtrainer.services.validation;

import com.backend.softtrainer.dtos.aiagent.AiGeneratedMessageDto;
import com.backend.softtrainer.dtos.aiagent.AiMessageGenerationResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 🛡️ AI Agent Response Validator
 * 
 * Validates all incoming AI-agent responses for:
 * - Schema validation (required fields, data types)
 * - Logic validation (business rules, consistency)
 * - Safety validation (content filtering, XSS prevention)
 * 
 * Ensures all AI responses meet platform standards before processing.
 */
@Service
@Slf4j
public class AiAgentResponseValidator {

    // Valid message types supported by the platform
    private static final Set<String> VALID_MESSAGE_TYPES = Set.of(
        "Text", 
        "SingleChoiceQuestion", 
        "MultipleChoiceQuestion", 
        "EnterTextQuestion", 
        "Hint", 
        "ResultSimulation"
    );

    // Content safety patterns (basic XSS prevention)
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile(
        "javascript:", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ON_EVENT_PATTERN = Pattern.compile(
        "on\\w+\\s*=", Pattern.CASE_INSENSITIVE
    );

    // Content limits
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int MAX_OPTION_LENGTH = 200;
    private static final int MAX_OPTIONS_COUNT = 10;
    private static final int MAX_MESSAGES_PER_RESPONSE = 5;

    /**
     * Validates a complete AI agent response
     * 
     * @param response The AI response to validate
     * @return ValidationResult containing success status and any errors
     */
    public ValidationResult validateResponse(AiMessageGenerationResponseDto response) {
        log.debug("🔍 Validating AI agent response");

        List<String> errors = new ArrayList<>();

        // 1. Schema Validation
        errors.addAll(validateSchema(response));

        // 2. Logic Validation
        errors.addAll(validateLogic(response));

        // 3. Safety Validation
        errors.addAll(validateSafety(response));

        boolean isValid = errors.isEmpty();
        
        if (isValid) {
            log.debug("✅ AI response validation passed");
        } else {
            log.warn("❌ AI response validation failed: {}", String.join(", ", errors));
        }

        return new ValidationResult(isValid, errors);
    }

    /**
     * Schema validation - ensures all required fields are present and properly formatted
     */
    private List<String> validateSchema(AiMessageGenerationResponseDto response) {
        List<String> errors = new ArrayList<>();

        // Validate top-level response
        if (response == null) {
            errors.add("Response cannot be null");
            return errors;
        }

        if (response.getSuccess() == null) {
            errors.add("Success field is required");
        }

        if (response.getMessages() == null) {
            errors.add("Messages array is required");
            return errors;
        }

        // Validate message count
        if (response.getMessages().size() > MAX_MESSAGES_PER_RESPONSE) {
            errors.add("Too many messages in response (max: " + MAX_MESSAGES_PER_RESPONSE + ")");
        }

        // Validate each message
        for (int i = 0; i < response.getMessages().size(); i++) {
            AiGeneratedMessageDto message = response.getMessages().get(i);
            List<String> messageErrors = validateMessageSchema(message, i);
            errors.addAll(messageErrors);
        }

        return errors;
    }

    /**
     * Validates schema for individual message
     */
    private List<String> validateMessageSchema(AiGeneratedMessageDto message, int index) {
        List<String> errors = new ArrayList<>();
        String prefix = "Message[" + index + "]: ";

        if (message == null) {
            errors.add(prefix + "Message cannot be null");
            return errors;
        }

        // Required fields
        if (isNullOrEmpty(message.getMessageType())) {
            errors.add(prefix + "messageType is required");
        } else if (!VALID_MESSAGE_TYPES.contains(message.getMessageType())) {
            errors.add(prefix + "Invalid messageType: " + message.getMessageType());
        }

        if (isNullOrEmpty(message.getContent())) {
            errors.add(prefix + "content is required");
        }

        if (isNullOrEmpty(message.getCharacterName())) {
            errors.add(prefix + "characterName is required");
        }

        // Message type specific validation
        if (message.getMessageType() != null) {
            errors.addAll(validateMessageTypeSpecificSchema(message, prefix));
        }

        return errors;
    }

    /**
     * Validates schema requirements specific to each message type
     */
    private List<String> validateMessageTypeSpecificSchema(AiGeneratedMessageDto message, String prefix) {
        List<String> errors = new ArrayList<>();
        String messageType = message.getMessageType();

        switch (messageType) {
            case "SingleChoiceQuestion":
            case "MultipleChoiceQuestion":
                if (message.getOptions() == null || message.getOptions().isEmpty()) {
                    errors.add(prefix + "Choice questions must have options");
                }
                if (message.getRequiresResponse() == null || !message.getRequiresResponse()) {
                    errors.add(prefix + "Question messages must require response");
                }
                break;

            case "EnterTextQuestion":
                if (message.getRequiresResponse() == null || !message.getRequiresResponse()) {
                    errors.add(prefix + "Question messages must require response");
                }
                break;

            case "Text":
            case "Hint":
                if (message.getRequiresResponse() != null && message.getRequiresResponse()) {
                    errors.add(prefix + "Text/Hint messages should not require response");
                }
                break;

            case "ResultSimulation":
                // ResultSimulation can optionally require response for feedback
                break;
        }

        return errors;
    }

    /**
     * Logic validation - ensures business rules and consistency
     */
    private List<String> validateLogic(AiMessageGenerationResponseDto response) {
        List<String> errors = new ArrayList<>();

        if (response.getMessages() == null || response.getMessages().isEmpty()) {
            return errors; // Already handled in schema validation
        }

        // Validate content length
        for (int i = 0; i < response.getMessages().size(); i++) {
            AiGeneratedMessageDto message = response.getMessages().get(i);
            String prefix = "Message[" + i + "]: ";

            if (message.getContent() != null && message.getContent().length() > MAX_CONTENT_LENGTH) {
                errors.add(prefix + "Content too long (max: " + MAX_CONTENT_LENGTH + " chars)");
            }

            // Validate options if present
            if (message.getOptions() != null) {
                if (message.getOptions().size() > MAX_OPTIONS_COUNT) {
                    errors.add(prefix + "Too many options (max: " + MAX_OPTIONS_COUNT + ")");
                }

                for (int j = 0; j < message.getOptions().size(); j++) {
                    String option = message.getOptions().get(j);
                    if (isNullOrEmpty(option)) {
                        errors.add(prefix + "Option[" + j + "] cannot be empty");
                    } else if (option.length() > MAX_OPTION_LENGTH) {
                        errors.add(prefix + "Option[" + j + "] too long (max: " + MAX_OPTION_LENGTH + " chars)");
                    }
                }

                // Check for duplicate options
                Set<String> uniqueOptions = new HashSet<>(message.getOptions());
                if (uniqueOptions.size() != message.getOptions().size()) {
                    errors.add(prefix + "Duplicate options detected");
                }
            }
        }

        // Business logic: At least one message should end with an actionable element
        // Skip this check if the AI agent is experiencing issues or failures
        boolean isFailureResponse = response.getSuccess() != null && !response.getSuccess();
        
        boolean isAiAgentUnavailable = isFailureResponse && 
                response.getErrorMessage() != null && response.getErrorMessage().contains("unavailable");
        
        // Also check if this is a fallback response (system error messages)
        boolean isFallbackResponse = response.getGenerationMetadata() != null && 
                response.getGenerationMetadata().containsKey("fallback") &&
                Boolean.TRUE.equals(response.getGenerationMetadata().get("fallback"));
        
        // Check if messages contain system error content indicating AI issues
        boolean hasSystemErrorContent = response.getMessages().stream()
                .anyMatch(m -> m.getContent() != null && 
                    (m.getContent().contains("technical difficulties") || 
                     m.getContent().contains("experiencing issues") ||
                     m.getContent().contains("try again") ||
                     m.getCharacterName() != null && m.getCharacterName().equals("System")));
        
        // Skip actionable message requirement if this is any kind of failure/error response
        boolean shouldSkipActionableCheck = isFailureResponse || isAiAgentUnavailable || 
                                          isFallbackResponse || hasSystemErrorContent;
        
        if (!shouldSkipActionableCheck) {
            boolean hasActionableMessage = response.getMessages().stream()
                    .anyMatch(m -> m.getRequiresResponse() != null && m.getRequiresResponse());

            if (!hasActionableMessage && !isConversationEnded(response)) {
                errors.add("Response should contain at least one actionable message (unless conversation ended)");
            }
        }

        return errors;
    }

    /**
     * Safety validation - content filtering and XSS prevention
     */
    private List<String> validateSafety(AiMessageGenerationResponseDto response) {
        List<String> errors = new ArrayList<>();

        if (response.getMessages() == null) {
            return errors;
        }

        for (int i = 0; i < response.getMessages().size(); i++) {
            AiGeneratedMessageDto message = response.getMessages().get(i);
            String prefix = "Message[" + i + "]: ";

            // Content safety
            if (message.getContent() != null) {
                errors.addAll(validateContentSafety(message.getContent(), prefix + "content"));
            }

            // Options safety
            if (message.getOptions() != null) {
                for (int j = 0; j < message.getOptions().size(); j++) {
                    String option = message.getOptions().get(j);
                    if (option != null) {
                        errors.addAll(validateContentSafety(option, prefix + "option[" + j + "]"));
                    }
                }
            }

            // Character name safety
            if (message.getCharacterName() != null) {
                errors.addAll(validateContentSafety(message.getCharacterName(), prefix + "characterName"));
            }
        }

        return errors;
    }

    /**
     * Validates content for security issues (XSS prevention)
     */
    private List<String> validateContentSafety(String content, String fieldName) {
        List<String> errors = new ArrayList<>();

        if (content == null || content.trim().isEmpty()) {
            return errors;
        }

        // Check for script tags
        if (SCRIPT_PATTERN.matcher(content).find()) {
            errors.add(fieldName + " contains potentially malicious script tags");
        }

        // Check for javascript: protocol
        if (JAVASCRIPT_PATTERN.matcher(content).find()) {
            errors.add(fieldName + " contains javascript: protocol");
        }

        // Check for on* event handlers
        if (ON_EVENT_PATTERN.matcher(content).find()) {
            errors.add(fieldName + " contains event handlers");
        }

        // Additional checks for common XSS patterns
        String lowerContent = content.toLowerCase();
        if (lowerContent.contains("<iframe") || lowerContent.contains("<object") || lowerContent.contains("<embed")) {
            errors.add(fieldName + " contains potentially unsafe HTML elements");
        }

        return errors;
    }

    /**
     * Checks if conversation is marked as ended
     */
    private boolean isConversationEnded(AiMessageGenerationResponseDto response) {
        return response.getConversationEnded() != null && response.getConversationEnded();
    }

    /**
     * Utility method to check if string is null or empty
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Validation result containing success status and error details
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = Collections.unmodifiableList(errors);
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
} 