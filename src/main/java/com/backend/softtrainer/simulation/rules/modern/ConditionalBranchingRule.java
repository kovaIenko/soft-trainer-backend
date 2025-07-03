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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🌳 Conditional Branching Rule - Complex flow control with nested conditions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class ConditionalBranchingRule implements FlowRule {
    
    public enum LogicOperator {
        AND, OR, NOT, XOR
    }
    
    public enum ConditionType {
        HYPERPARAMETER, MESSAGE_COUNT, USER_RESPONSE, TIME_BASED, VARIABLE, NESTED
    }
    
    public enum ComparisonOperator {
        EQUALS("=="), NOT_EQUALS("!="), GREATER_THAN(">"), LESS_THAN("<"), 
        GREATER_EQUAL(">="), LESS_EQUAL("<="), CONTAINS("contains"), 
        NOT_CONTAINS("not_contains"), IN("in"), NOT_IN("not_in");
        
        private final String symbol;
        ComparisonOperator(String symbol) { this.symbol = symbol; }
        public String getSymbol() { return symbol; }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Condition {
        @JsonProperty("type")
        private ConditionType type;
        @JsonProperty("parameter")
        private String parameter;
        @JsonProperty("operator")
        private ComparisonOperator operator;
        @JsonProperty("value")
        private Object value;
        @JsonProperty("nested_rule")
        private ConditionalBranchingRule nestedRule;
        @JsonProperty("description")
        private String description;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Action {
        @JsonProperty("type")
        private String type;
        @JsonProperty("name")
        private String name;
        @JsonProperty("value")
        private Object value;
        @JsonProperty("target_node_id")
        private Long targetNodeId;
        @JsonProperty("description")
        private String description;
    }
    
    @JsonProperty("rule_id")
    private String ruleId;
    
    @JsonProperty("logic_operator")
    @Builder.Default
    private LogicOperator logicOperator = LogicOperator.AND;
    
    @JsonProperty("conditions")
    private List<Condition> conditions;
    
    @JsonProperty("actions")
    private List<Action> actions;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("priority_boost")
    @Builder.Default
    private Integer priorityBoost = 0;
    
    @Builder.Default
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
    
    @Override
    public boolean evaluate(SimulationContext context) {
        log.debug("🌳 Evaluating ConditionalBranching rule: {} with {} conditions", 
            ruleId, conditions != null ? conditions.size() : 0);
        
        try {
            boolean result = evaluateConditions(context);
            
            if (result && actions != null && !actions.isEmpty()) {
                executeActions(context);
            }
            
            log.debug("🌳 Conditional evaluation result: {} -> {}", ruleId, result);
            return result;
            
        } catch (Exception e) {
            log.error("❌ Error evaluating conditional branching rule {}: {}", ruleId, e.getMessage());
            return false;
        }
    }
    
    private boolean evaluateConditions(SimulationContext context) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        
        return switch (logicOperator) {
            case AND -> conditions.stream().allMatch(c -> evaluateCondition(c, context));
            case OR -> conditions.stream().anyMatch(c -> evaluateCondition(c, context));
            case NOT -> !conditions.stream().allMatch(c -> evaluateCondition(c, context));
            case XOR -> conditions.stream().mapToInt(c -> evaluateCondition(c, context) ? 1 : 0).sum() == 1;
        };
    }
    
    private boolean evaluateCondition(Condition condition, SimulationContext context) {
        return switch (condition.type) {
            case HYPERPARAMETER -> evaluateHyperParameterCondition(condition, context);
            case MESSAGE_COUNT -> evaluateMessageCountCondition(condition, context);
            case USER_RESPONSE -> evaluateUserResponseCondition(condition, context);
            case TIME_BASED -> evaluateTimeBasedCondition(condition, context);
            case VARIABLE -> evaluateVariableCondition(condition, context);
            case NESTED -> evaluateNestedCondition(condition, context);
        };
    }
    
    private boolean evaluateHyperParameterCondition(Condition condition, SimulationContext context) {
        Double actualValue = context.getHyperParameter(condition.parameter);
        Double expectedValue = convertToDouble(condition.value);
        
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        
        return compareNumbers(actualValue, expectedValue, condition.operator);
    }
    
    private boolean evaluateMessageCountCondition(Condition condition, SimulationContext context) {
        int actualCount = context.getMessageCount();
        Integer expectedCount = convertToInteger(condition.value);
        
        if (expectedCount == null) {
            return false;
        }
        
        return compareNumbers(actualCount, expectedCount, condition.operator);
    }
    
    private boolean evaluateUserResponseCondition(Condition condition, SimulationContext context) {
        List<Integer> userSelections = context.getUserSelections(Long.valueOf(condition.parameter));
        List<Integer> expectedSelections = convertToIntegerList(condition.value);
        
        if (userSelections == null || expectedSelections == null) {
            return false;
        }
        
        return switch (condition.operator) {
            case EQUALS -> userSelections.equals(expectedSelections);
            case CONTAINS -> userSelections.containsAll(expectedSelections);
            case NOT_CONTAINS -> !userSelections.containsAll(expectedSelections);
            default -> false;
        };
    }
    
    private boolean evaluateTimeBasedCondition(Condition condition, SimulationContext context) {
        long actualTime = context.getDurationSeconds();
        Long expectedTime = convertToLong(condition.value);
        
        if (expectedTime == null) {
            return false;
        }
        
        return compareNumbers(actualTime, expectedTime, condition.operator);
    }
    
    private boolean evaluateVariableCondition(Condition condition, SimulationContext context) {
        Object actualValue = variables.get(condition.parameter);
        Object expectedValue = condition.value;
        
        if (actualValue == null) {
            return expectedValue == null;
        }
        
        return switch (condition.operator) {
            case EQUALS -> actualValue.equals(expectedValue);
            case NOT_EQUALS -> !actualValue.equals(expectedValue);
            default -> false;
        };
    }
    
    private boolean evaluateNestedCondition(Condition condition, SimulationContext context) {
        if (condition.nestedRule == null) {
            return false;
        }
        
        return condition.nestedRule.evaluate(context);
    }
    
    /**
     * 🔢 Compare numeric values using the specified operator
     */
    private boolean compareNumbers(Number actual, Number expected, ComparisonOperator operator) {
        if (actual == null || expected == null) return false;
        
        double actualDouble = actual.doubleValue();
        double expectedDouble = expected.doubleValue();
        
        return switch (operator) {
            case EQUALS -> Double.compare(actualDouble, expectedDouble) == 0;
            case NOT_EQUALS -> Double.compare(actualDouble, expectedDouble) != 0;
            case GREATER_THAN -> actualDouble > expectedDouble;
            case LESS_THAN -> actualDouble < expectedDouble;
            case GREATER_EQUAL -> actualDouble >= expectedDouble;
            case LESS_EQUAL -> actualDouble <= expectedDouble;
            default -> false;
        };
    }
    
    private void executeActions(SimulationContext context) {
        if (actions == null) return;
        
        for (Action action : actions) {
            try {
                executeAction(action, context);
            } catch (Exception e) {
                log.error("❌ Error executing action {}: {}", action.type, e.getMessage());
            }
        }
    }
    
    private void executeAction(Action action, SimulationContext context) {
        switch (action.type) {
            case "set_variable" -> variables.put(action.name, action.value);
            case "set_hyperparameter" -> {
                Double value = convertToDouble(action.value);
                if (value != null) {
                    context.setHyperParameter(action.name, value);
                }
            }
            case "log_message" -> log.info("🎬 Action executed: {}", action.value);
            case "mark_completed" -> context.markAsCompleted();
            default -> log.warn("⚠️ Unknown action type: {}", action.type);
        }
    }
    
    // Utility conversion methods
    private Double convertToDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try { return Double.parseDouble((String) value); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
    
    private Integer convertToInteger(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try { return Integer.parseInt((String) value); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
    
    private Long convertToLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try { return Long.parseLong((String) value); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private List<Integer> convertToIntegerList(Object value) {
        if (value instanceof List) {
            try {
                return ((List<?>) value).stream()
                    .map(item -> convertToInteger(item))
                    .filter(item -> item != null)
                    .toList();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
    
    @Override
    public String getDescription() {
        if (description != null && !description.trim().isEmpty()) {
            return description;
        }
        
        return String.format("Conditional branching with %s logic (%d conditions)", 
            logicOperator, conditions != null ? conditions.size() : 0);
    }
    
    @Override
    public String getRuleId() {
        return ruleId != null ? ruleId : "conditional_" + logicOperator.name().toLowerCase();
    }
    
    @Override
    public int getPriority() {
        return 8 + priorityBoost;
    }
    
    public static ConditionalBranchingRule ifThenElse(Condition condition, List<Action> thenActions) {
        return ConditionalBranchingRule.builder()
            .ruleId("if_then_" + System.nanoTime())
            .logicOperator(LogicOperator.AND)
            .conditions(List.of(condition))
            .actions(thenActions)
            .description("If-then conditional logic")
            .build();
    }
    
    public static ConditionalBranchingRule multiCondition(LogicOperator operator, List<Condition> conditions, List<Action> actions) {
        return ConditionalBranchingRule.builder()
            .ruleId("multi_" + operator.name().toLowerCase() + "_" + System.nanoTime())
            .logicOperator(operator)
            .conditions(conditions)
            .actions(actions)
            .description(String.format("Multi-condition %s logic", operator))
            .build();
    }
}
