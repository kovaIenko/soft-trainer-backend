package com.backend.softtrainer.services.validation;

import com.backend.softtrainer.dtos.aiagent.AiGeneratedMessageDto;
import com.backend.softtrainer.dtos.aiagent.AiMessageGenerationResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class AiAgentResponseValidatorTest {

    private AiAgentResponseValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AiAgentResponseValidator();
    }

    @Test
    void testEnterTextQuestionWithNullContentShouldPass() {
        // Create a response with EnterTextQuestion that has null content
        AiMessageGenerationResponseDto response = AiMessageGenerationResponseDto.builder()
                .messages(Arrays.asList(
                    AiGeneratedMessageDto.builder()
                            .messageType("Text")
                            .content("Welcome message")
                            .characterName("AI Assistant")
                            .requiresResponse(false)
                            .build(),
                    AiGeneratedMessageDto.builder()
                            .messageType("EnterTextQuestion")
                            .content(null) // This should be allowed
                            .characterName("AI Assistant")
                            .requiresResponse(true)
                            .build()
                ))
                .success(true)
                .build();

        // Validate the response
        AiAgentResponseValidator.ValidationResult result = validator.validateResponse(response);

        // Should pass validation
        assertTrue(result.isValid(), "EnterTextQuestion with null content should pass validation");
        assertTrue(result.getErrors().isEmpty(), "Should have no validation errors");
    }

    @Test
    void testTextMessageWithNullContentShouldFail() {
        // Create a response with Text message that has null content
        AiMessageGenerationResponseDto response = AiMessageGenerationResponseDto.builder()
                .messages(Arrays.asList(
                    AiGeneratedMessageDto.builder()
                            .messageType("Text")
                            .content(null) // This should fail
                            .characterName("AI Assistant")
                            .requiresResponse(false)
                            .build()
                ))
                .success(true)
                .build();

        // Validate the response
        AiAgentResponseValidator.ValidationResult result = validator.validateResponse(response);

        // Should fail validation
        assertFalse(result.isValid(), "Text message with null content should fail validation");
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.contains("content is required")), 
                "Should have content validation error");
    }

    @Test
    void testSingleChoiceQuestionWithNullContentShouldFail() {
        // Create a response with SingleChoiceQuestion that has null content
        AiMessageGenerationResponseDto response = AiMessageGenerationResponseDto.builder()
                .messages(Arrays.asList(
                    AiGeneratedMessageDto.builder()
                            .messageType("SingleChoiceQuestion")
                            .content(null) // This should fail
                            .characterName("AI Assistant")
                            .requiresResponse(true)
                            .options(Arrays.asList("Option 1", "Option 2"))
                            .build()
                ))
                .success(true)
                .build();

        // Validate the response
        AiAgentResponseValidator.ValidationResult result = validator.validateResponse(response);

        // Should fail validation
        assertFalse(result.isValid(), "SingleChoiceQuestion with null content should fail validation");
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.contains("content is required")), 
                "Should have content validation error");
    }

    @Test
    void testMixedMessageTypesWithCorrectContent() {
        // Create a response with mixed message types
        AiMessageGenerationResponseDto response = AiMessageGenerationResponseDto.builder()
                .messages(Arrays.asList(
                    AiGeneratedMessageDto.builder()
                            .messageType("Text")
                            .content("Welcome message")
                            .characterName("AI Assistant")
                            .requiresResponse(false)
                            .build(),
                    AiGeneratedMessageDto.builder()
                            .messageType("Text")
                            .content("Question content")
                            .characterName("AI Assistant")
                            .requiresResponse(false)
                            .build(),
                    AiGeneratedMessageDto.builder()
                            .messageType("EnterTextQuestion")
                            .content(null) // This should be allowed
                            .characterName("AI Assistant")
                            .requiresResponse(true)
                            .build()
                ))
                .success(true)
                .build();

        // Validate the response
        AiAgentResponseValidator.ValidationResult result = validator.validateResponse(response);

        // Should pass validation
        assertTrue(result.isValid(), "Mixed message types with correct content should pass validation");
        assertTrue(result.getErrors().isEmpty(), "Should have no validation errors");
    }
} 