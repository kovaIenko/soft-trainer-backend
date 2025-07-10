package com.backend.softtrainer.entities.flow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 🔍 Rule Condition Entity
 * 
 * Represents a condition that must be met for a rule to trigger
 * in the modern simulation format.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuleCondition {
    
    /**
     * Type of condition to evaluate
     */
    private String type;
    
    /**
     * Option ID for OPTION_SELECTED conditions
     */
    @JsonProperty("option_id")
    private String optionId;
    
    /**
     * Hyperparameter key for HYPERPARAMETER_THRESHOLD conditions
     */
    private String key;
    
    /**
     * Minimum value for threshold conditions
     */
    @JsonProperty("min_value")
    private Integer minValue;
    
    /**
     * Maximum value for threshold conditions
     */
    @JsonProperty("max_value")
    private Integer maxValue;
    
    /**
     * Actions to execute when this condition is met
     */
    private List<RuleAction> actions;
    
    /**
     * Condition Type Constants
     */
    public static class ConditionType {
        public static final String OPTION_SELECTED = "OPTION_SELECTED";
        public static final String HYPERPARAMETER_THRESHOLD = "HYPERPARAMETER_THRESHOLD";
        public static final String MESSAGE_COUNT = "MESSAGE_COUNT";
        public static final String TEXT_CONTAINS = "TEXT_CONTAINS";
    }
} 