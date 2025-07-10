package com.backend.softtrainer.simulation.rules.modern;

import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.enums.SimulationMode;
import com.backend.softtrainer.simulation.context.SimulationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 🧪 Unit Tests for HyperParameterActionRule
 */
@DisplayName("HyperParameterActionRule Tests")
class HyperParameterActionRuleTest {

    private SimulationContext context;

    @BeforeEach
    void setUp() {
        Chat testChat = Chat.builder()
            .id(1L)
            .hearts(5.0)
            .build();

        context = SimulationContext.builder()
            .chatId(1L)
            .chat(testChat)
            .simulationMode(SimulationMode.PREDEFINED)
            .hearts(5.0)
            .build();

        // Set initial parameter values
        context.setHyperParameter("empathy", 3.0);
        context.setHyperParameter("engagement", 2.5);
    }

    @Test
    @DisplayName("Should increment hyperparameter correctly")
    void shouldIncrementHyperParameter() {
        // Given
        HyperParameterActionRule rule = HyperParameterActionRule.builder()
            .ruleId("test_increment")
            .type(HyperParameterActionRule.ActionType.INCREMENT)
            .parameter("empathy")
            .value(2.0)
            .build();

        // When
        boolean result = rule.evaluate(context);

        // Then
        assertTrue(result);
        assertEquals(5.0, context.getHyperParameter("empathy"));
    }

    @Test
    @DisplayName("Should decrement hyperparameter correctly")
    void shouldDecrementHyperParameter() {
        // Given
        HyperParameterActionRule rule = HyperParameterActionRule.builder()
            .ruleId("test_decrement")
            .type(HyperParameterActionRule.ActionType.DECREMENT)
            .parameter("engagement")
            .value(1.0)
            .build();

        // When
        boolean result = rule.evaluate(context);

        // Then
        assertTrue(result);
        assertEquals(1.5, context.getHyperParameter("engagement"));
    }

    @Test
    @DisplayName("Should set hyperparameter to specific value")
    void shouldSetHyperParameter() {
        // Given
        HyperParameterActionRule rule = HyperParameterActionRule.builder()
            .ruleId("test_set")
            .type(HyperParameterActionRule.ActionType.SET)
            .parameter("empathy")
            .value(7.0)
            .build();

        // When
        boolean result = rule.evaluate(context);

        // Then
        assertTrue(result);
        assertEquals(7.0, context.getHyperParameter("empathy"));
    }

    @Test
    @DisplayName("Should multiply hyperparameter correctly")
    void shouldMultiplyHyperParameter() {
        // Given
        HyperParameterActionRule rule = HyperParameterActionRule.builder()
            .ruleId("test_multiply")
            .type(HyperParameterActionRule.ActionType.MULTIPLY)
            .parameter("empathy")
            .value(2.0)
            .build();

        // When
        boolean result = rule.evaluate(context);

        // Then
        assertTrue(result);
        assertEquals(6.0, context.getHyperParameter("empathy"));
    }

    @Test
    @DisplayName("Should apply minimum constraints")
    void shouldApplyMinimumConstraints() {
        // Given
        HyperParameterActionRule rule = HyperParameterActionRule.builder()
            .ruleId("test_min_constraint")
            .type(HyperParameterActionRule.ActionType.DECREMENT)
            .parameter("empathy")
            .value(5.0)
            .minValue(1.0)
            .build();

        // When
        boolean result = rule.evaluate(context);

        // Then
        assertTrue(result);
        assertEquals(1.0, context.getHyperParameter("empathy")); // Should be constrained to min
    }

    @Test
    @DisplayName("Should apply maximum constraints")
    void shouldApplyMaximumConstraints() {
        // Given
        HyperParameterActionRule rule = HyperParameterActionRule.builder()
            .ruleId("test_max_constraint")
            .type(HyperParameterActionRule.ActionType.INCREMENT)
            .parameter("empathy")
            .value(10.0)
            .maxValue(8.0)
            .build();

        // When
        boolean result = rule.evaluate(context);

        // Then
        assertTrue(result);
        assertEquals(8.0, context.getHyperParameter("empathy")); // Should be constrained to max
    }

    @Test
    @DisplayName("Should use builder methods for common actions")
    void shouldUseBuilderMethods() {
        // Given
        HyperParameterActionRule incrementRule = HyperParameterActionRule.increment("empathy", 1.5);
        HyperParameterActionRule decrementRule = HyperParameterActionRule.decrement("engagement", 0.5);
        HyperParameterActionRule setRule = HyperParameterActionRule.set("new_param", 4.0);

        // When/Then
        assertTrue(incrementRule.evaluate(context));
        assertEquals(4.5, context.getHyperParameter("empathy"));

        assertTrue(decrementRule.evaluate(context));
        assertEquals(2.0, context.getHyperParameter("engagement"));

        assertTrue(setRule.evaluate(context));
        assertEquals(4.0, context.getHyperParameter("new_param"));
    }

    @Test
    @DisplayName("Should handle null parameter gracefully")
    void shouldHandleNullParameter() {
        // Given
        HyperParameterActionRule rule = HyperParameterActionRule.builder()
            .ruleId("test_null")
            .type(HyperParameterActionRule.ActionType.INCREMENT)
            .parameter("nonexistent")
            .value(2.0)
            .build();

        // When
        boolean result = rule.evaluate(context);

        // Then
        assertTrue(result);
        assertEquals(2.0, context.getHyperParameter("nonexistent")); // Should start from 0.0
    }
}
