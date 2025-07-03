package com.backend.softtrainer.simulation.rules;

import com.backend.softtrainer.simulation.context.SimulationContext;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * JSON-configurable flow rule that replaces hardcoded showPredicate strings
 */
@Data
@AllArgsConstructor
public class JsonFlowRule implements FlowRule {
    
    public enum RuleType {
        MESSAGE_COUNT, USER_RESPONSE_COUNT, HYPER_PARAMETER, ALWAYS_TRUE
    }
    
    public enum Operator {
        EQUALS, GREATER_THAN, LESS_THAN
    }
    
    private final String ruleId;
    private final RuleType type;
    private final Operator operator;
    private final Object value;
    private final String field;
    private final String description;
    
    @Override
    public boolean evaluate(SimulationContext context) {
        Object contextValue = extractContextValue(context);
        return compareValues(contextValue, value, operator);
    }
    
    private Object extractContextValue(SimulationContext context) {
        return switch (type) {
            case MESSAGE_COUNT -> context.getMessageCount();
            case USER_RESPONSE_COUNT -> (int) context.getMessageHistory().stream()
                .filter(m -> "USER".equals(m.getRole().name()))
                .count();
            case HYPER_PARAMETER -> context.getHyperParameter(field);
            case ALWAYS_TRUE -> true;
        };
    }
    
    private boolean compareValues(Object contextValue, Object expectedValue, Operator op) {
        if (contextValue instanceof Number && expectedValue instanceof Number) {
            double ctx = ((Number) contextValue).doubleValue();
            double exp = ((Number) expectedValue).doubleValue();
            
            return switch (op) {
                case EQUALS -> ctx == exp;
                case GREATER_THAN -> ctx > exp;
                case LESS_THAN -> ctx < exp;
            };
        }
        return false;
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