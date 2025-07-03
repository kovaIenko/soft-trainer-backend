package com.backend.softtrainer.simulation.rules;

import com.backend.softtrainer.simulation.context.SimulationContext;

/**
 * Interface for flow control rules that replace the opaque showPredicate system
 */
public interface FlowRule {
    
    /**
     * Evaluate if this rule passes for the given context
     * 
     * @param context Current simulation context
     * @return true if the rule passes and flow should continue
     */
    boolean evaluate(SimulationContext context);
    
    /**
     * Get a human-readable description of what this rule does
     * 
     * @return rule description for debugging/documentation
     */
    String getDescription();
    
    /**
     * Get rule priority (higher values evaluated first)
     * 
     * @return priority value
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * Get rule identifier for caching/debugging
     * 
     * @return unique rule identifier
     */
    String getRuleId();
} 