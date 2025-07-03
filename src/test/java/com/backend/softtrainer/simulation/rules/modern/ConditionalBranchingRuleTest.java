package com.backend.softtrainer.simulation.rules.modern;

import com.backend.softtrainer.simulation.context.SimulationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConditionalBranchingRule Tests")
class ConditionalBranchingRuleTest {
    
    private SimulationContext context;
    
    @BeforeEach
    void setUp() {
        context = SimulationContext.builder()
            .chatId(1L)
            .hearts(5.0)
            .build();
        
        // Set some test hyperparameters
        context.setHyperParameter("empathy", 7.0);
        context.setHyperParameter("engagement", 3.0);
    }
    
    @Test
    @DisplayName("Should create if-then-else rule")
    void shouldCreateIfThenElseRule() {
        ConditionalBranchingRule.Condition condition = ConditionalBranchingRule.Condition.builder()
            .type(ConditionalBranchingRule.ConditionType.HYPERPARAMETER)
            .parameter("empathy")
            .operator(ConditionalBranchingRule.ComparisonOperator.GREATER_THAN)
            .value(5.0)
            .build();
        
        ConditionalBranchingRule.Action action = ConditionalBranchingRule.Action.builder()
            .type("set_variable")
            .name("high_empathy")
            .value(true)
            .build();
        
        ConditionalBranchingRule rule = ConditionalBranchingRule.ifThenElse(
            condition, List.of(action));
        
        assertEquals(ConditionalBranchingRule.LogicOperator.AND, rule.getLogicOperator());
        assertEquals(1, rule.getConditions().size());
        assertEquals(1, rule.getActions().size());
        assertTrue(rule.getRuleId().startsWith("if_then_"));
    }
    
    @Test
    @DisplayName("Should create multi-condition rule")
    void shouldCreateMultiConditionRule() {
        ConditionalBranchingRule.Condition condition1 = ConditionalBranchingRule.Condition.builder()
            .type(ConditionalBranchingRule.ConditionType.HYPERPARAMETER)
            .parameter("empathy")
            .operator(ConditionalBranchingRule.ComparisonOperator.GREATER_THAN)
            .value(5.0)
            .build();
        
        ConditionalBranchingRule.Condition condition2 = ConditionalBranchingRule.Condition.builder()
            .type(ConditionalBranchingRule.ConditionType.HYPERPARAMETER)
            .parameter("engagement")
            .operator(ConditionalBranchingRule.ComparisonOperator.GREATER_EQUAL)
            .value(3.0)
            .build();
        
        ConditionalBranchingRule.Action action = ConditionalBranchingRule.Action.builder()
            .type("mark_completed")
            .build();
        
        ConditionalBranchingRule rule = ConditionalBranchingRule.multiCondition(
            ConditionalBranchingRule.LogicOperator.AND,
            List.of(condition1, condition2),
            List.of(action)
        );
        
        assertEquals(ConditionalBranchingRule.LogicOperator.AND, rule.getLogicOperator());
        assertEquals(2, rule.getConditions().size());
        assertTrue(rule.getRuleId().startsWith("multi_and_"));
    }
    
    @Test
    @DisplayName("Should evaluate AND logic correctly")
    void shouldEvaluateAndLogicCorrectly() {
        ConditionalBranchingRule.Condition condition1 = ConditionalBranchingRule.Condition.builder()
            .type(ConditionalBranchingRule.ConditionType.HYPERPARAMETER)
            .parameter("empathy")
            .operator(ConditionalBranchingRule.ComparisonOperator.GREATER_THAN)
            .value(5.0) // 7.0 > 5.0 = true
            .build();
        
        ConditionalBranchingRule.Condition condition2 = ConditionalBranchingRule.Condition.builder()
            .type(ConditionalBranchingRule.ConditionType.HYPERPARAMETER)
            .parameter("engagement")
            .operator(ConditionalBranchingRule.ComparisonOperator.GREATER_EQUAL)
            .value(3.0) // 3.0 >= 3.0 = true
            .build();
        
        ConditionalBranchingRule rule = ConditionalBranchingRule.builder()
            .logicOperator(ConditionalBranchingRule.LogicOperator.AND)
            .conditions(List.of(condition1, condition2))
            .build();
        
        assertTrue(rule.evaluate(context)); // true AND true = true
        
        // Change second condition to make it false
        condition2.setValue(5.0); // 3.0 >= 5.0 = false
        assertFalse(rule.evaluate(context)); // true AND false = false
    }
    
    @Test
    @DisplayName("Should evaluate OR logic correctly")
    void shouldEvaluateOrLogicCorrectly() {
        ConditionalBranchingRule.Condition condition1 = ConditionalBranchingRule.Condition.builder()
            .type(ConditionalBranchingRule.ConditionType.HYPERPARAMETER)
            .parameter("empathy")
            .operator(ConditionalBranchingRule.ComparisonOperator.GREATER_THAN)
            .value(10.0) // 7.0 > 10.0 = false
            .build();
        
        ConditionalBranchingRule.Condition condition2 = ConditionalBranchingRule.Condition.builder()
            .type(ConditionalBranchingRule.ConditionType.HYPERPARAMETER)
            .parameter("engagement")
            .operator(ConditionalBranchingRule.ComparisonOperator.GREATER_EQUAL)
            .value(3.0) // 3.0 >= 3.0 = true
            .build();
        
        ConditionalBranchingRule rule = ConditionalBranchingRule.builder()
            .logicOperator(ConditionalBranchingRule.LogicOperator.OR)
            .conditions(List.of(condition1, condition2))
            .build();
        
        assertTrue(rule.evaluate(context)); // false OR true = true
        
        // Make both conditions false
        condition2.setValue(5.0); // 3.0 >= 5.0 = false
        assertFalse(rule.evaluate(context)); // false OR false = false
    }
    
    @Test
    @DisplayName("Should evaluate XOR logic correctly")
    void shouldEvaluateXorLogicCorrectly() {
        ConditionalBranchingRule.Condition condition1 = ConditionalBranchingRule.Condition.builder()
            .type(ConditionalBranchingRule.ConditionType.HYPERPARAMETER)
            .parameter("empathy")
            .operator(ConditionalBranchingRule.ComparisonOperator.GREATER_THAN)
            .value(5.0) // 7.0 > 5.0 = true
            .build();
        
        ConditionalBranchingRule.Condition condition2 = ConditionalBranchingRule.Condition.builder()
            .type(ConditionalBranchingRule.ConditionType.HYPERPARAMETER)
            .parameter("engagement")
            .operator(ConditionalBranchingRule.ComparisonOperator.GREATER_THAN)
            .value(5.0) // 3.0 > 5.0 = false
            .build();
        
        ConditionalBranchingRule rule = ConditionalBranchingRule.builder()
            .logicOperator(ConditionalBranchingRule.LogicOperator.XOR)
            .conditions(List.of(condition1, condition2))
            .build();
        
        assertTrue(rule.evaluate(context)); // true XOR false = true
        
        // Make both conditions true
        condition2.setValue(2.0); // 3.0 > 2.0 = true
        assertFalse(rule.evaluate(context)); // true XOR true = false
    }
    
    @Test
    @DisplayName("Should evaluate NOT logic correctly")
    void shouldEvaluateNotLogicCorrectly() {
        ConditionalBranchingRule.Condition condition = ConditionalBranchingRule.Condition.builder()
            .type(ConditionalBranchingRule.ConditionType.HYPERPARAMETER)
            .parameter("empathy")
            .operator(ConditionalBranchingRule.ComparisonOperator.GREATER_THAN)
            .value(5.0) // 7.0 > 5.0 = true
            .build();
        
        ConditionalBranchingRule rule = ConditionalBranchingRule.builder()
            .logicOperator(ConditionalBranchingRule.LogicOperator.NOT)
            .conditions(List.of(condition))
            .build();
        
        assertFalse(rule.evaluate(context)); // NOT true = false
        
        condition.setValue(10.0); // 7.0 > 10.0 = false
        assertTrue(rule.evaluate(context)); // NOT false = true
    }
    
    @Test
    @DisplayName("Should handle different comparison operators")
    void shouldHandleDifferentComparisonOperators() {
        // Test EQUALS
        ConditionalBranchingRule.Condition equalsCondition = ConditionalBranchingRule.Condition.builder()
            .type(ConditionalBranchingRule.ConditionType.HYPERPARAMETER)
            .parameter("empathy")
            .operator(ConditionalBranchingRule.ComparisonOperator.EQUALS)
            .value(7.0)
            .build();
        
        ConditionalBranchingRule rule = ConditionalBranchingRule.builder()
            .conditions(List.of(equalsCondition))
            .build();
        
        assertTrue(rule.evaluate(context));
        
        // Test NOT_EQUALS
        equalsCondition.setOperator(ConditionalBranchingRule.ComparisonOperator.NOT_EQUALS);
        assertFalse(rule.evaluate(context));
        
        // Test LESS_THAN
        equalsCondition.setOperator(ConditionalBranchingRule.ComparisonOperator.LESS_THAN);
        equalsCondition.setValue(10.0);
        assertTrue(rule.evaluate(context)); // 7.0 < 10.0
        
        // Test LESS_EQUAL
        equalsCondition.setOperator(ConditionalBranchingRule.ComparisonOperator.LESS_EQUAL);
        equalsCondition.setValue(7.0);
        assertTrue(rule.evaluate(context)); // 7.0 <= 7.0
    }
    
    @Test
    @DisplayName("Should execute actions when conditions are met")
    void shouldExecuteActionsWhenConditionsMet() {
        ConditionalBranchingRule.Condition condition = ConditionalBranchingRule.Condition.builder()
            .type(ConditionalBranchingRule.ConditionType.HYPERPARAMETER)
            .parameter("empathy")
            .operator(ConditionalBranchingRule.ComparisonOperator.GREATER_THAN)
            .value(5.0)
            .build();
        
        ConditionalBranchingRule.Action action1 = ConditionalBranchingRule.Action.builder()
            .type("set_variable")
            .name("test_var")
            .value("test_value")
            .build();
        
        ConditionalBranchingRule.Action action2 = ConditionalBranchingRule.Action.builder()
            .type("set_hyperparameter")
            .name("bonus_points")
            .value(10.0)
            .build();
        
        ConditionalBranchingRule rule = ConditionalBranchingRule.builder()
            .conditions(List.of(condition))
            .actions(List.of(action1, action2))
            .build();
        
        assertTrue(rule.evaluate(context));
        
        // Check that actions were executed
        assertEquals("test_value", rule.getVariables().get("test_var"));
        assertEquals(10.0, context.getHyperParameter("bonus_points"));
    }
    
    @Test
    @DisplayName("Should not execute actions when conditions are not met")
    void shouldNotExecuteActionsWhenConditionsNotMet() {
        ConditionalBranchingRule.Condition condition = ConditionalBranchingRule.Condition.builder()
            .type(ConditionalBranchingRule.ConditionType.HYPERPARAMETER)
            .parameter("empathy")
            .operator(ConditionalBranchingRule.ComparisonOperator.GREATER_THAN)
            .value(10.0) // 7.0 > 10.0 = false
            .build();
        
        ConditionalBranchingRule.Action action = ConditionalBranchingRule.Action.builder()
            .type("set_variable")
            .name("test_var")
            .value("test_value")
            .build();
        
        ConditionalBranchingRule rule = ConditionalBranchingRule.builder()
            .conditions(List.of(condition))
            .actions(List.of(action))
            .build();
        
        assertFalse(rule.evaluate(context));
        
        // Check that action was not executed
        assertNull(rule.getVariables().get("test_var"));
    }
    
    @Test
    @DisplayName("Should evaluate message count conditions")
    void shouldEvaluateMessageCountConditions() {
        ConditionalBranchingRule.Condition condition = ConditionalBranchingRule.Condition.builder()
            .type(ConditionalBranchingRule.ConditionType.MESSAGE_COUNT)
            .operator(ConditionalBranchingRule.ComparisonOperator.EQUALS)
            .value(0) // Context starts with 0 messages
            .build();
        
        ConditionalBranchingRule rule = ConditionalBranchingRule.builder()
            .conditions(List.of(condition))
            .build();
        
        assertTrue(rule.evaluate(context));
        
        // Add a message and test again
        // Note: This would require adding a message to the context
        condition.setValue(1);
        assertFalse(rule.evaluate(context)); // Still 0 messages
    }
    
    @Test
    @DisplayName("Should handle variable conditions")
    void shouldHandleVariableConditions() {
        ConditionalBranchingRule rule = ConditionalBranchingRule.builder().build();
        
        // Set a variable
        rule.getVariables().put("test_flag", true);
        
        ConditionalBranchingRule.Condition condition = ConditionalBranchingRule.Condition.builder()
            .type(ConditionalBranchingRule.ConditionType.VARIABLE)
            .parameter("test_flag")
            .operator(ConditionalBranchingRule.ComparisonOperator.EQUALS)
            .value(true)
            .build();
        
        rule.setConditions(List.of(condition));
        
        assertTrue(rule.evaluate(context));
        
        // Test with different value
        condition.setValue(false);
        assertFalse(rule.evaluate(context));
    }
    
    @Test
    @DisplayName("Should have correct priority")
    void shouldHaveCorrectPriority() {
        ConditionalBranchingRule rule = ConditionalBranchingRule.builder().build();
        assertEquals(8, rule.getPriority());
        
        rule.setPriorityBoost(2);
        assertEquals(10, rule.getPriority());
    }
    
    @Test
    @DisplayName("Should generate proper descriptions")
    void shouldGenerateProperDescriptions() {
        ConditionalBranchingRule rule1 = ConditionalBranchingRule.builder()
            .logicOperator(ConditionalBranchingRule.LogicOperator.AND)
            .conditions(List.of(
                ConditionalBranchingRule.Condition.builder().build(),
                ConditionalBranchingRule.Condition.builder().build()
            ))
            .build();
        
        assertTrue(rule1.getDescription().contains("AND logic"));
        assertTrue(rule1.getDescription().contains("2 conditions"));
        
        ConditionalBranchingRule rule2 = ConditionalBranchingRule.builder()
            .description("Custom description")
            .build();
        
        assertEquals("Custom description", rule2.getDescription());
    }
    
    @Test
    @DisplayName("Should handle null safety in conditions")
    void shouldHandleNullSafetyInConditions() {
        // Test with null conditions list
        ConditionalBranchingRule rule = ConditionalBranchingRule.builder()
            .conditions(null)
            .build();
        
        assertTrue(rule.evaluate(context)); // Empty conditions should return true
        
        // Test with condition having null parameter
        ConditionalBranchingRule.Condition condition = ConditionalBranchingRule.Condition.builder()
            .type(ConditionalBranchingRule.ConditionType.HYPERPARAMETER)
            .parameter("nonexistent_param")
            .operator(ConditionalBranchingRule.ComparisonOperator.GREATER_THAN)
            .value(5.0)
            .build();
        
        rule.setConditions(List.of(condition));
        assertFalse(rule.evaluate(context)); // Should handle null parameter gracefully
    }
}
