package com.backend.softtrainer.simulation.validation;

import com.backend.softtrainer.simulation.rules.FlowRule;
import com.backend.softtrainer.simulation.rules.modern.HyperParameterActionRule;
import com.backend.softtrainer.simulation.rules.modern.UserResponseRule;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🔍 Rule Validation Service - Ensures rule integrity and provides testing utilities
 * 
 * Validates:
 * - Rule syntax and structure
 * - Parameter constraints
 * - Logic consistency
 * - Performance implications
 * - Integration compatibility
 */
@Service
@Slf4j
public class RuleValidationService {
    
    /**
     * 🔍 Validate a single flow rule
     */
    public RuleValidationResult validateRule(FlowRule rule) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        try {
            // Basic null checks
            if (rule == null) {
                issues.add(ValidationIssue.error("RULE_NULL", "Rule cannot be null"));
                return RuleValidationResult.invalid(issues);
            }
            
            // Validate rule ID
            if (rule.getRuleId() == null || rule.getRuleId().trim().isEmpty()) {
                issues.add(ValidationIssue.error("MISSING_RULE_ID", "Rule must have a valid ID"));
            }
            
            // Type-specific validation
            if (rule instanceof HyperParameterActionRule hyperRule) {
                validateHyperParameterRule(hyperRule, issues);
            } else if (rule instanceof UserResponseRule userRule) {
                validateUserResponseRule(userRule, issues);
            }
            
            // Generic rule validation
            validateRuleStructure(rule, issues);
            
        } catch (Exception e) {
            log.error("🚨 Unexpected error validating rule: {}", rule.getRuleId(), e);
            issues.add(ValidationIssue.error("VALIDATION_EXCEPTION", 
                "Validation failed: " + e.getMessage()));
        }
        
        return issues.isEmpty() ? 
            RuleValidationResult.valid() : 
            RuleValidationResult.invalid(issues);
    }
    
    /**
     * 🔍 Validate JSON rule configuration
     */
    public RuleValidationResult validateJsonRule(JsonNode ruleJson) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        if (ruleJson == null) {
            issues.add(ValidationIssue.error("JSON_NULL", "Rule JSON cannot be null"));
            return RuleValidationResult.invalid(issues);
        }
        
        // Validate required fields
        if (!ruleJson.has("type")) {
            issues.add(ValidationIssue.error("MISSING_TYPE", "Rule must specify a type"));
        }
        
        String ruleType = ruleJson.path("type").asText();
        
        switch (ruleType) {
            case "user_response":
                validateJsonUserResponseRule(ruleJson, issues);
                break;
            case "hyperparameter":
                validateJsonHyperParameterRule(ruleJson, issues);
                break;
            default:
                issues.add(ValidationIssue.warning("UNKNOWN_TYPE", 
                    "Unknown rule type: " + ruleType));
        }
        
        return issues.isEmpty() ? 
            RuleValidationResult.valid() : 
            RuleValidationResult.invalid(issues);
    }
    
    /**
     * 🔍 Validate complete rule set for consistency
     */
    public RuleSetValidationResult validateRuleSet(List<FlowRule> rules) {
        List<ValidationIssue> issues = new ArrayList<>();
        Map<String, FlowRule> ruleMap = new HashMap<>();
        
        // Check for duplicate rule IDs
        for (FlowRule rule : rules) {
            if (rule.getRuleId() != null) {
                if (ruleMap.containsKey(rule.getRuleId())) {
                    issues.add(ValidationIssue.error("DUPLICATE_RULE_ID", 
                        "Duplicate rule ID: " + rule.getRuleId()));
                } else {
                    ruleMap.put(rule.getRuleId(), rule);
                }
            }
        }
        
        // Validate individual rules
        Map<String, RuleValidationResult> ruleResults = new HashMap<>();
        for (int i = 0; i < rules.size(); i++) {
            FlowRule rule = rules.get(i);
            String key = rule.getRuleId() != null ? rule.getRuleId() : "unknown_" + i;
            // Handle duplicates by appending index
            while (ruleResults.containsKey(key)) {
                key = key + "_" + i;
            }
            ruleResults.put(key, validateRule(rule));
        }
        
        // Check for logical conflicts
        validateRuleLogicConsistency(rules, issues);
        
        // Check for performance implications
        validatePerformanceImplications(rules, issues);
        
        return RuleSetValidationResult.builder()
            .overallValid(issues.stream().noneMatch(i -> i.getSeverity() == Severity.ERROR))
            .globalIssues(issues)
            .ruleResults(ruleResults)
            .totalRules(rules.size())
            .validRules((int) ruleResults.values().stream().mapToLong(r -> r.isValid() ? 1 : 0).sum())
            .build();
    }
    
    /**
     * 🧪 Test rule against simulation context
     */
    public RuleTestResult testRule(FlowRule rule, SimulationContext context) {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        String errorMessage = null;
        boolean result = false;
        
        try {
            result = rule.evaluate(context);
            success = true;
        } catch (Exception e) {
            log.warn("🚨 Rule test failed: {}", rule.getRuleId(), e);
            errorMessage = e.getMessage();
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        return RuleTestResult.builder()
            .ruleId(rule.getRuleId())
            .success(success)
            .result(result)
            .executionTime(executionTime)
            .errorMessage(errorMessage)
            .contextHash(generateContextHash(context))
            .build();
    }
    
    /**
     * 🧪 Batch test multiple rules
     */
    public List<RuleTestResult> testRules(List<FlowRule> rules, SimulationContext context) {
        return rules.stream()
            .map(rule -> testRule(rule, context))
            .collect(Collectors.toList());
    }
    
    // Private validation methods
    
    private void validateHyperParameterRule(HyperParameterActionRule rule, List<ValidationIssue> issues) {
        if (rule.getParameter() == null || rule.getParameter().trim().isEmpty()) {
            issues.add(ValidationIssue.error("MISSING_PARAMETER", 
                "HyperParameter rule must specify a parameter name"));
        }
        
        if (rule.getType() == null) {
            issues.add(ValidationIssue.error("MISSING_ACTION_TYPE", 
                "HyperParameter rule must specify an action type"));
        }
        
        if (rule.getValue() == null) {
            issues.add(ValidationIssue.error("MISSING_VALUE", 
                "HyperParameter rule must specify a value"));
        }
        
        // Validate constraints
        if (rule.getMinValue() != null && rule.getMaxValue() != null) {
            if (rule.getMinValue() > rule.getMaxValue()) {
                issues.add(ValidationIssue.error("INVALID_CONSTRAINTS", 
                    "Min value cannot be greater than max value"));
            }
        }
    }
    
    private void validateUserResponseRule(UserResponseRule rule, List<ValidationIssue> issues) {
        if (rule.getMessageId() == null) {
            issues.add(ValidationIssue.error("MISSING_MESSAGE_ID", 
                "UserResponse rule must specify a message ID"));
        }
        
        if (rule.getExpectedOptions() == null || rule.getExpectedOptions().isEmpty()) {
            issues.add(ValidationIssue.warning("NO_EXPECTED_OPTIONS", 
                "UserResponse rule has no expected options"));
        }
        
        if (rule.getMatchType() == null) {
            issues.add(ValidationIssue.warning("MISSING_MATCH_TYPE", 
                "UserResponse rule should specify a match type"));
        }
    }
    
    private void validateRuleStructure(FlowRule rule, List<ValidationIssue> issues) {
        // Validate description length
        if (rule.getDescription() != null && rule.getDescription().length() > 500) {
            issues.add(ValidationIssue.warning("LONG_DESCRIPTION", 
                "Rule description is very long (>500 chars)"));
        }
        
        // Validate priority range
        if (rule.getPriority() < 0 || rule.getPriority() > 100) {
            issues.add(ValidationIssue.warning("INVALID_PRIORITY", 
                "Rule priority should be between 0 and 100"));
        }
    }
    
    private void validateJsonUserResponseRule(JsonNode ruleJson, List<ValidationIssue> issues) {
        if (!ruleJson.has("message_id")) {
            issues.add(ValidationIssue.error("MISSING_MESSAGE_ID", 
                "UserResponse rule must have message_id"));
        }
        
        if (!ruleJson.has("expected_options")) {
            issues.add(ValidationIssue.warning("MISSING_EXPECTED_OPTIONS", 
                "UserResponse rule should have expected_options"));
        } else {
            JsonNode options = ruleJson.get("expected_options");
            if (!options.isArray()) {
                issues.add(ValidationIssue.error("INVALID_OPTIONS_FORMAT", 
                    "expected_options must be an array"));
            }
        }
    }
    
    private void validateJsonHyperParameterRule(JsonNode ruleJson, List<ValidationIssue> issues) {
        if (!ruleJson.has("parameter")) {
            issues.add(ValidationIssue.error("MISSING_PARAMETER", 
                "HyperParameter rule must have parameter"));
        }
        
        if (!ruleJson.has("operator")) {
            issues.add(ValidationIssue.error("MISSING_OPERATOR", 
                "HyperParameter rule must have operator"));
        }
        
        if (!ruleJson.has("value")) {
            issues.add(ValidationIssue.error("MISSING_VALUE", 
                "HyperParameter rule must have value"));
        }
    }
    
    private void validateRuleLogicConsistency(List<FlowRule> rules, List<ValidationIssue> issues) {
        // Check for conflicting hyperparameter actions
        Map<String, List<HyperParameterActionRule>> parameterActions = rules.stream()
            .filter(rule -> rule instanceof HyperParameterActionRule)
            .map(rule -> (HyperParameterActionRule) rule)
            .collect(Collectors.groupingBy(HyperParameterActionRule::getParameter));
        
        parameterActions.forEach((parameter, actions) -> {
            if (actions.size() > 1) {
                // Check for conflicting SET operations
                long setCount = actions.stream()
                    .filter(action -> action.getType() == HyperParameterActionRule.ActionType.SET)
                    .count();
                
                if (setCount > 1) {
                    issues.add(ValidationIssue.error("CONFLICTING_SET_ACTIONS", 
                        "Multiple SET actions for parameter: " + parameter));
                }
            }
        });
    }
    
    private void validatePerformanceImplications(List<FlowRule> rules, List<ValidationIssue> issues) {
        // Warn about too many rules
        if (rules.size() > 50) {
            issues.add(ValidationIssue.warning("MANY_RULES", 
                "Large number of rules (" + rules.size() + ") may impact performance"));
        }
        
        // Check for complex rule patterns that might be slow
        long complexRules = rules.stream()
            .filter(this::isComplexRule)
            .count();
        
        if (complexRules > 10) {
            issues.add(ValidationIssue.warning("MANY_COMPLEX_RULES", 
                "Many complex rules may impact performance"));
        }
    }
    
    private boolean isComplexRule(FlowRule rule) {
        // Define what makes a rule "complex"
        return rule.getDescription() != null && rule.getDescription().length() > 200;
    }
    
    private String generateContextHash(SimulationContext context) {
        return String.valueOf(Objects.hash(
            context.getChatId(),
            context.getHearts(),
            context.getMessageHistory().size()
        ));
    }
    
    // Data classes
    
    @Data
    @Builder
    public static class RuleValidationResult {
        private boolean valid;
        private List<ValidationIssue> issues;
        
        public static RuleValidationResult valid() {
            return RuleValidationResult.builder()
                .valid(true)
                .issues(new ArrayList<>())
                .build();
        }
        
        public static RuleValidationResult invalid(List<ValidationIssue> issues) {
            return RuleValidationResult.builder()
                .valid(false)
                .issues(issues)
                .build();
        }
    }
    
    @Data
    @Builder
    public static class RuleSetValidationResult {
        private boolean overallValid;
        private List<ValidationIssue> globalIssues;
        private Map<String, RuleValidationResult> ruleResults;
        private int totalRules;
        private int validRules;
    }
    
    @Data
    @Builder
    public static class RuleTestResult {
        private String ruleId;
        private boolean success;
        private boolean result;
        private long executionTime;
        private String errorMessage;
        private String contextHash;
    }
    
    @Data
    @Builder
    public static class ValidationIssue {
        private String code;
        private String message;
        private Severity severity;
        
        public static ValidationIssue error(String code, String message) {
            return ValidationIssue.builder()
                .code(code)
                .message(message)
                .severity(Severity.ERROR)
                .build();
        }
        
        public static ValidationIssue warning(String code, String message) {
            return ValidationIssue.builder()
                .code(code)
                .message(message)
                .severity(Severity.WARNING)
                .build();
        }
        
        public static ValidationIssue info(String code, String message) {
            return ValidationIssue.builder()
                .code(code)
                .message(message)
                .severity(Severity.INFO)
                .build();
        }
    }
    
    public enum Severity {
        ERROR, WARNING, INFO
    }
} 