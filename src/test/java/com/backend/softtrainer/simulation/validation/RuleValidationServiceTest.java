package com.backend.softtrainer.simulation.validation;

import com.backend.softtrainer.simulation.rules.modern.HyperParameterActionRule;
import com.backend.softtrainer.simulation.rules.modern.UserResponseRule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RuleValidationService Tests")
class RuleValidationServiceTest {
    
    private RuleValidationService validationService;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        validationService = new RuleValidationService();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    @DisplayName("Should validate valid hyperparameter rule")
    void shouldValidateValidHyperParameterRule() {
        HyperParameterActionRule rule = HyperParameterActionRule.builder()
            .ruleId("test_rule")
            .type(HyperParameterActionRule.ActionType.INCREMENT)
            .parameter("empathy")
            .value(2.0)
            .build();
        
        RuleValidationService.RuleValidationResult result = validationService.validateRule(rule);
        
        assertTrue(result.isValid());
        assertTrue(result.getIssues().isEmpty());
    }
    
    @Test
    @DisplayName("Should detect invalid hyperparameter rule")
    void shouldDetectInvalidHyperParameterRule() {
        HyperParameterActionRule rule = HyperParameterActionRule.builder()
            .ruleId("") // Invalid empty ID
            .type(null) // Missing type
            .parameter(null) // Missing parameter
            .value(null) // Missing value
            .build();
        
        RuleValidationService.RuleValidationResult result = validationService.validateRule(rule);
        
        assertFalse(result.isValid());
        assertTrue(result.getIssues().size() > 0);
        
        boolean hasRuleIdError = result.getIssues().stream()
            .anyMatch(issue -> issue.getCode().equals("MISSING_RULE_ID"));
        assertTrue(hasRuleIdError);
    }
    
    @Test
    @DisplayName("Should validate constraint consistency")
    void shouldValidateConstraintConsistency() {
        HyperParameterActionRule rule = HyperParameterActionRule.builder()
            .ruleId("constraint_test")
            .type(HyperParameterActionRule.ActionType.SET)
            .parameter("test_param")
            .value(5.0)
            .minValue(10.0) // Min > Max - should be invalid
            .maxValue(8.0)
            .build();
        
        RuleValidationService.RuleValidationResult result = validationService.validateRule(rule);
        
        assertFalse(result.isValid());
        boolean hasConstraintError = result.getIssues().stream()
            .anyMatch(issue -> issue.getCode().equals("INVALID_CONSTRAINTS"));
        assertTrue(hasConstraintError);
    }
    
    @Test
    @DisplayName("Should validate JSON rule structure")
    void shouldValidateJsonRuleStructure() throws Exception {
        String validJson = """
            {
                "type": "user_response",
                "message_id": 5,
                "expected_options": [1, 2, 3]
            }
            """;
        
        JsonNode ruleJson = objectMapper.readTree(validJson);
        RuleValidationService.RuleValidationResult result = validationService.validateJsonRule(ruleJson);
        
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("Should detect invalid JSON rule")
    void shouldDetectInvalidJsonRule() throws Exception {
        String invalidJson = """
            {
                "message_id": 5,
                "expected_options": "not_an_array"
            }
            """;
        
        JsonNode ruleJson = objectMapper.readTree(invalidJson);
        RuleValidationService.RuleValidationResult result = validationService.validateJsonRule(ruleJson);
        
        assertFalse(result.isValid());
        assertTrue(result.getIssues().size() > 0);
    }
    
    @Test
    @DisplayName("Should validate rule set consistency")
    void shouldValidateRuleSetConsistency() {
        HyperParameterActionRule rule1 = HyperParameterActionRule.builder()
            .ruleId("rule1")
            .type(HyperParameterActionRule.ActionType.SET)
            .parameter("empathy")
            .value(5.0)
            .build();
        
        HyperParameterActionRule rule2 = HyperParameterActionRule.builder()
            .ruleId("rule2")
            .type(HyperParameterActionRule.ActionType.SET)
            .parameter("empathy")
            .value(7.0)
            .build();
        
        List<com.backend.softtrainer.simulation.rules.FlowRule> rules = Arrays.asList(rule1, rule2);
        RuleValidationService.RuleSetValidationResult result = validationService.validateRuleSet(rules);
        
        assertFalse(result.isOverallValid());
        assertTrue(result.getGlobalIssues().size() > 0);
        
        boolean hasConflictWarning = result.getGlobalIssues().stream()
            .anyMatch(issue -> issue.getCode().equals("CONFLICTING_SET_ACTIONS"));
        assertTrue(hasConflictWarning);
    }
    
    @Test
    @DisplayName("Should detect duplicate rule IDs")
    void shouldDetectDuplicateRuleIds() {
        HyperParameterActionRule rule1 = HyperParameterActionRule.builder()
            .ruleId("duplicate_id")
            .type(HyperParameterActionRule.ActionType.INCREMENT)
            .parameter("empathy")
            .value(1.0)
            .build();
        
        HyperParameterActionRule rule2 = HyperParameterActionRule.builder()
            .ruleId("duplicate_id")
            .type(HyperParameterActionRule.ActionType.INCREMENT)
            .parameter("engagement")
            .value(1.0)
            .build();
        
        List<com.backend.softtrainer.simulation.rules.FlowRule> rules = Arrays.asList(rule1, rule2);
        RuleValidationService.RuleSetValidationResult result = validationService.validateRuleSet(rules);
        
        assertFalse(result.isOverallValid());
        
        boolean hasDuplicateError = result.getGlobalIssues().stream()
            .anyMatch(issue -> issue.getCode().equals("DUPLICATE_RULE_ID"));
        assertTrue(hasDuplicateError);
    }
    
    @Test
    @DisplayName("Should handle null rule validation")
    void shouldHandleNullRule() {
        RuleValidationService.RuleValidationResult result = validationService.validateRule(null);
        
        assertFalse(result.isValid());
        assertEquals(1, result.getIssues().size());
        assertEquals("RULE_NULL", result.getIssues().get(0).getCode());
    }
    
    @Test
    @DisplayName("Should handle null JSON validation")
    void shouldHandleNullJson() {
        RuleValidationService.RuleValidationResult result = validationService.validateJsonRule(null);
        
        assertFalse(result.isValid());
        assertEquals(1, result.getIssues().size());
        assertEquals("JSON_NULL", result.getIssues().get(0).getCode());
    }
} 