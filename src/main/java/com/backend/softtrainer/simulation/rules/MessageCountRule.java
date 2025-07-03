package com.backend.softtrainer.simulation.rules;

import com.backend.softtrainer.simulation.context.SimulationContext;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Rule that evaluates based on the number of messages in the chat
 */
@Data
@AllArgsConstructor
public class MessageCountRule implements FlowRule {
    
    public enum Operator {
        EQUALS, GREATER_THAN, LESS_THAN, GREATER_OR_EQUAL, LESS_OR_EQUAL
    }
    
    private final String ruleId;
    private final Operator operator;
    private final int threshold;
    private final String description;
    
    public MessageCountRule(String ruleId, Operator operator, int threshold) {
        this.ruleId = ruleId;
        this.operator = operator;
        this.threshold = threshold;
        this.description = String.format("Message count %s %d", operator.name().toLowerCase(), threshold);
    }
    
    @Override
    public boolean evaluate(SimulationContext context) {
        int messageCount = context.getMessageCount();
        
        return switch (operator) {
            case EQUALS -> messageCount == threshold;
            case GREATER_THAN -> messageCount > threshold;
            case LESS_THAN -> messageCount < threshold;
            case GREATER_OR_EQUAL -> messageCount >= threshold;
            case LESS_OR_EQUAL -> messageCount <= threshold;
        };
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public String getRuleId() {
        return ruleId;
    }
} 