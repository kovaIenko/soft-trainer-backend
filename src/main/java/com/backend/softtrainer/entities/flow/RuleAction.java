package com.backend.softtrainer.entities.flow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * ⚡ Rule Action Entity
 * 
 * Represents an action to be executed when a rule condition is met
 * in the modern simulation format.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuleAction {
    
    /**
     * Type of action to execute
     */
    private String type;
    
    /**
     * Hyperparameter key (for hyperparameter actions)
     */
    private String key;
    
    /**
     * Value to set or modify (for hyperparameter actions)
     */
    private Integer value;
    
    /**
     * Message to show (for message actions)
     */
    private String message;
    
    /**
     * Target message ID (for navigation actions)
     */
    private Long targetMessageId;
    
    /**
     * Action Type Constants
     */
    public static class ActionType {
        public static final String INCREASE_HYPERPARAMETER = "INCREASE_HYPERPARAMETER";
        public static final String DECREASE_HYPERPARAMETER = "DECREASE_HYPERPARAMETER";
        public static final String SET_HYPERPARAMETER = "SET_HYPERPARAMETER";
        public static final String SHOW_MESSAGE = "SHOW_MESSAGE";
        public static final String NAVIGATE_TO_MESSAGE = "NAVIGATE_TO_MESSAGE";
        public static final String END_SIMULATION = "END_SIMULATION";
    }
} 