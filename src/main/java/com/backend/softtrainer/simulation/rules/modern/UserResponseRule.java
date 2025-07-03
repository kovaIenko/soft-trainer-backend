package com.backend.softtrainer.simulation.rules.modern;

import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.simulation.rules.FlowRule;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * 👤 User Response Rule - Validates user choices and input
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class UserResponseRule implements FlowRule {
    
    public enum MatchType {
        EXACT_MATCH, CONTAINS_ANY, CONTAINS_ALL, NOT_CONTAINS, 
        TEXT_CONTAINS, TEXT_REGEX, RANGE_CHECK
    }
    
    @JsonProperty("rule_id")
    private String ruleId;
    
    @JsonProperty("message_id")
    private Long messageId;
    
    @JsonProperty("expected_options")
    private List<Integer> expectedOptions;
    
    @JsonProperty("match_type")
    @Builder.Default
    private MatchType matchType = MatchType.EXACT_MATCH;
    
    @JsonProperty("description")
    private String description;
    
    @Override
    public boolean evaluate(SimulationContext context) {
        log.debug("👤 Evaluating UserResponse rule for message: {}", messageId);
        
        try {
            List<Integer> userSelections = context.getUserSelections(messageId);
            
            return switch (matchType) {
                case EXACT_MATCH -> evaluateExactMatch(userSelections);
                case CONTAINS_ANY -> evaluateContainsAny(userSelections);
                case CONTAINS_ALL -> evaluateContainsAll(userSelections);
                case NOT_CONTAINS -> evaluateNotContains(userSelections);
                default -> false;
            };
            
        } catch (Exception e) {
            log.error("❌ Error evaluating user response rule {}: {}", ruleId, e.getMessage());
            return false;
        }
    }
    
    private boolean evaluateExactMatch(List<Integer> userSelections) {
        if (userSelections == null || expectedOptions == null) return false;
        
        Set<Integer> userSet = new HashSet<>(userSelections);
        Set<Integer> expectedSet = new HashSet<>(expectedOptions);
        
        return userSet.equals(expectedSet);
    }
    
    private boolean evaluateContainsAny(List<Integer> userSelections) {
        if (userSelections == null || expectedOptions == null) return false;
        return userSelections.stream().anyMatch(expectedOptions::contains);
    }
    
    private boolean evaluateContainsAll(List<Integer> userSelections) {
        if (userSelections == null || expectedOptions == null) return false;
        return userSelections.containsAll(expectedOptions);
    }
    
    private boolean evaluateNotContains(List<Integer> userSelections) {
        if (userSelections == null) return expectedOptions == null || expectedOptions.isEmpty();
        if (expectedOptions == null) return true;
        return userSelections.stream().noneMatch(expectedOptions::contains);
    }
    
    @Override
    public String getDescription() {
        return description != null ? description : 
            String.format("Validate user response for message %d using %s", messageId, matchType);
    }
    
    @Override
    public String getRuleId() {
        return ruleId != null ? ruleId : "user_response_" + messageId;
    }
    
    @Override
    public int getPriority() {
        return 5;
    }
    
    public static UserResponseRule exactMatch(Long messageId, List<Integer> expectedOptions) {
        return UserResponseRule.builder()
            .ruleId("exact_match_" + messageId)
            .messageId(messageId)
            .expectedOptions(expectedOptions)
            .matchType(MatchType.EXACT_MATCH)
            .build();
    }
}
