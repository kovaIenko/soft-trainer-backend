package com.backend.softtrainer.simulation.rules.modern;

import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.simulation.rules.FlowRule;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 📈 HyperParameter Action Rule - Executes hyperparameter modifications
 * 
 * This rule replaces complex predicate operations like:
 * "saveChatValue[\"active_listening\", readChatValue[\"active_listening\"]+2]"
 * 
 * With clear JSON structure:
 * {
 *   "type": "INCREMENT",
 *   "parameter": "active_listening",
 *   "value": 2.0,
 *   "description": "Reward active listening behavior"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class HyperParameterActionRule implements FlowRule {
    
    public enum ActionType {
        SET,          // Set parameter to specific value
        INCREMENT,    // Add value to current parameter
        DECREMENT,    // Subtract value from current parameter
        MULTIPLY,     // Multiply current parameter by value
        MIN,          // Set to minimum of current and value
        MAX           // Set to maximum of current and value
    }
    
    @JsonProperty("rule_id")
    private String ruleId;
    
    @JsonProperty("type")
    private ActionType type;
    
    @JsonProperty("parameter")
    private String parameter;
    
    @JsonProperty("value")
    private Double value;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("condition")
    private String condition; // Optional condition for execution
    
    @JsonProperty("min_value")
    private Double minValue; // Optional minimum constraint
    
    @JsonProperty("max_value")
    private Double maxValue; // Optional maximum constraint
    
    @Override
    public boolean evaluate(SimulationContext context) {
        log.debug("📈 Executing HyperParameterAction: {} on parameter {}", 
            type, parameter);
        
        try {
            // Check optional condition
            if (condition != null && !evaluateCondition(context)) {
                log.debug("🚫 Condition not met for hyperparameter action: {}", condition);
                return true; // Rule passes but action not executed
            }
            
            // Get current value
            Double currentValue = context.getHyperParameter(parameter);
            
            // Calculate new value based on action type
            Double newValue = calculateNewValue(currentValue, value, type);
            
            // Apply constraints
            newValue = applyConstraints(newValue);
            
            // Update the parameter
            context.setHyperParameter(parameter, newValue);
            
            log.info("📈 Updated hyperparameter {}: {} -> {} ({})", 
                parameter, currentValue, newValue, type);
            
            return true;
            
        } catch (Exception e) {
            log.error("❌ Error executing hyperparameter action {}: {}", 
                ruleId, e.getMessage());
            return false;
        }
    }
    
    /**
     * 🧮 Calculate new value based on action type
     */
    private Double calculateNewValue(Double current, Double actionValue, ActionType actionType) {
        if (current == null) current = 0.0;
        if (actionValue == null) actionValue = 0.0;
        
        return switch (actionType) {
            case SET -> actionValue;
            case INCREMENT -> current + actionValue;
            case DECREMENT -> current - actionValue;
            case MULTIPLY -> current * actionValue;
            case MIN -> Math.min(current, actionValue);
            case MAX -> Math.max(current, actionValue);
        };
    }
    
    /**
     * 🔒 Apply min/max constraints to the new value
     */
    private Double applyConstraints(Double value) {
        if (minValue != null && value < minValue) {
            log.debug("🔒 Applied minimum constraint: {} -> {}", value, minValue);
            value = minValue;
        }
        
        if (maxValue != null && value > maxValue) {
            log.debug("🔒 Applied maximum constraint: {} -> {}", value, maxValue);
            value = maxValue;
        }
        
        return value;
    }
    
    /**
     * ❓ Evaluate optional condition (simplified)
     */
    private boolean evaluateCondition(SimulationContext context) {
        // Simple condition evaluation - can be enhanced
        if (condition.contains(">")) {
            String[] parts = condition.split(">");
            if (parts.length == 2) {
                String param = parts[0].trim();
                Double threshold = Double.parseDouble(parts[1].trim());
                return context.getHyperParameter(param) > threshold;
            }
        }
        
        // Default to true for unknown conditions
        return true;
    }
    
    @Override
    public String getDescription() {
        if (description != null && !description.trim().isEmpty()) {
            return description;
        }
        
        return String.format("%s %s by %s", type.name(), parameter, value);
    }
    
    @Override
    public String getRuleId() {
        return ruleId != null ? ruleId : type.name().toLowerCase() + "_" + parameter;
    }
    
    @Override
    public int getPriority() {
        return 10; // Higher priority for hyperparameter actions
    }
    
    /**
     * 🏗️ Builder methods for common actions
     */
    public static HyperParameterActionRule increment(String parameter, Double value) {
        return HyperParameterActionRule.builder()
            .ruleId("increment_" + parameter)
            .type(ActionType.INCREMENT)
            .parameter(parameter)
            .value(value)
            .description("Increment " + parameter + " by " + value)
            .build();
    }
    
    public static HyperParameterActionRule decrement(String parameter, Double value) {
        return HyperParameterActionRule.builder()
            .ruleId("decrement_" + parameter)
            .type(ActionType.DECREMENT)
            .parameter(parameter)
            .value(value)
            .description("Decrement " + parameter + " by " + value)
            .build();
    }
    
    public static HyperParameterActionRule set(String parameter, Double value) {
        return HyperParameterActionRule.builder()
            .ruleId("set_" + parameter)
            .type(ActionType.SET)
            .parameter(parameter)
            .value(value)
            .description("Set " + parameter + " to " + value)
            .build();
    }
} 