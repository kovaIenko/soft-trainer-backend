package com.backend.softtrainer.entities.flow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 🔧 Modern Flow Rule Entity
 * 
 * Represents a rule in the modern simulation format that determines
 * when messages should be shown and what actions to execute.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowRule {
    
    /**
     * Rule type that determines how the rule should be evaluated
     */
    private String type;
    
    /**
     * Human-readable description of what this rule does
     */
    private String description;
    
    /**
     * Previous message ID this rule depends on (for DEPENDS_ON_PREVIOUS type)
     */
    private Long previousMessageId;
    
    /**
     * Conditions that must be met for this rule to trigger
     */
    private List<RuleCondition> conditions;
    
    /**
     * Actions to execute when this rule is triggered
     */
    private List<RuleAction> actions;
    
    /**
     * Rule Type Constants
     */
    public static class RuleType {
        public static final String ALWAYS_SHOW = "ALWAYS_SHOW";
        public static final String DEPENDS_ON_PREVIOUS = "DEPENDS_ON_PREVIOUS";
        public static final String CONDITIONAL_BRANCHING = "CONDITIONAL_BRANCHING";
        public static final String ANSWER_QUALITY_RULE = "ANSWER_QUALITY_RULE";
        public static final String FINAL_EVALUATION = "FINAL_EVALUATION";
    }
} 