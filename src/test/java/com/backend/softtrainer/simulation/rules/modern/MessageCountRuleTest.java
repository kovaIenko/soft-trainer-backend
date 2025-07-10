package com.backend.softtrainer.simulation.rules.modern;

import com.backend.softtrainer.entities.enums.ChatRole;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.simulation.context.SimulationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MessageCountRule Tests")
class MessageCountRuleTest {

    private SimulationContext context;

    @BeforeEach
    void setUp() {
        context = SimulationContext.builder()
            .chatId(1L)
            .hearts(5.0)
            .build();
    }

    @Test
    @DisplayName("Should create minimum total messages rule")
    void shouldCreateMinTotalMessagesRule() {
        MessageCountRule rule = MessageCountRule.minTotalMessages(5);

        assertEquals("min_total_5", rule.getRuleId());
        assertEquals(MessageCountRule.CountType.TOTAL_MESSAGES, rule.getCountType());
        assertEquals(MessageCountRule.ComparisonType.GREATER_EQUAL, rule.getComparisonType());
        assertEquals(5, rule.getThreshold());
    }

    @Test
    @DisplayName("Should create maximum user messages rule")
    void shouldCreateMaxUserMessagesRule() {
        MessageCountRule rule = MessageCountRule.maxUserMessages(10);

        assertEquals("max_user_10", rule.getRuleId());
        assertEquals(MessageCountRule.CountType.USER_MESSAGES, rule.getCountType());
        assertEquals(MessageCountRule.ComparisonType.LESS_EQUAL, rule.getComparisonType());
        assertEquals(10, rule.getThreshold());
    }

    @Test
    @DisplayName("Should create between counts rule")
    void shouldCreateBetweenCountsRule() {
        MessageCountRule rule = MessageCountRule.betweenCounts(
            MessageCountRule.CountType.QUESTION_MESSAGES, 3, 8);

        assertEquals("between_question_messages_3_8", rule.getRuleId());
        assertEquals(MessageCountRule.CountType.QUESTION_MESSAGES, rule.getCountType());
        assertEquals(MessageCountRule.ComparisonType.BETWEEN, rule.getComparisonType());
        assertEquals(3, rule.getMinCount());
        assertEquals(8, rule.getMaxCount());
    }

    @Test
    @DisplayName("Should evaluate total message count correctly")
    void shouldEvaluateTotalMessageCount() {
        // Add 3 messages to context
        addMessage(MessageType.TEXT, ChatRole.APP);
        addMessage(MessageType.SINGLE_CHOICE_QUESTION, ChatRole.APP);
        addMessage(MessageType.SINGLE_CHOICE_ANSWER, ChatRole.USER);

        MessageCountRule rule = MessageCountRule.builder()
            .countType(MessageCountRule.CountType.TOTAL_MESSAGES)
            .comparisonType(MessageCountRule.ComparisonType.EQUALS)
            .threshold(3)
            .build();

        assertTrue(rule.evaluate(context));

        // Test with different threshold
        rule.setThreshold(5);
        assertFalse(rule.evaluate(context));
    }

    @Test
    @DisplayName("Should count user messages only")
    void shouldCountUserMessagesOnly() {
        // Add mixed messages
        addMessage(MessageType.TEXT, ChatRole.APP);
        addMessage(MessageType.SINGLE_CHOICE_QUESTION, ChatRole.APP);
        addMessage(MessageType.SINGLE_CHOICE_ANSWER, ChatRole.USER);
        addMessage(MessageType.ENTER_TEXT_ANSWER, ChatRole.USER);

        MessageCountRule rule = MessageCountRule.builder()
            .countType(MessageCountRule.CountType.USER_MESSAGES)
            .comparisonType(MessageCountRule.ComparisonType.EQUALS)
            .threshold(2)
            .build();

        assertTrue(rule.evaluate(context));
    }

    @Test
    @DisplayName("Should evaluate between range correctly")
    void shouldEvaluateBetweenRange() {
        // Add 5 messages
        for (int i = 0; i < 5; i++) {
            addMessage(MessageType.TEXT, ChatRole.APP);
        }

        MessageCountRule rule = MessageCountRule.builder()
            .countType(MessageCountRule.CountType.TOTAL_MESSAGES)
            .comparisonType(MessageCountRule.ComparisonType.BETWEEN)
            .minCount(3)
            .maxCount(7)
            .build();

        assertTrue(rule.evaluate(context));

        // Test outside range
        rule.setMinCount(6);
        rule.setMaxCount(10);
        assertFalse(rule.evaluate(context));
    }

    @Test
    @DisplayName("Should have correct priority")
    void shouldHaveCorrectPriority() {
        MessageCountRule rule = MessageCountRule.minTotalMessages(1);
        assertEquals(3, rule.getPriority());
    }

    @Test
    @DisplayName("Should generate proper descriptions")
    void shouldGenerateProperDescriptions() {
        MessageCountRule rule1 = MessageCountRule.minTotalMessages(5);
        assertTrue(rule1.getDescription().contains("at least 5 total messages"));

        MessageCountRule rule2 = MessageCountRule.betweenCounts(
            MessageCountRule.CountType.USER_MESSAGES, 2, 8);
        assertTrue(rule2.getDescription().contains("between 2 and 8"));
    }

    @Test
    @DisplayName("Should handle comparison types correctly")
    void shouldHandleComparisonTypes() {
        // Add 5 messages
        for (int i = 0; i < 5; i++) {
            addMessage(MessageType.TEXT, ChatRole.APP);
        }

        // Test GREATER_THAN
        MessageCountRule rule = MessageCountRule.builder()
            .countType(MessageCountRule.CountType.TOTAL_MESSAGES)
            .comparisonType(MessageCountRule.ComparisonType.GREATER_THAN)
            .threshold(4)
            .build();
        assertTrue(rule.evaluate(context));

        // Test LESS_THAN
        rule.setComparisonType(MessageCountRule.ComparisonType.LESS_THAN);
        rule.setThreshold(6);
        assertTrue(rule.evaluate(context));

        // Test NOT_EQUALS
        rule.setComparisonType(MessageCountRule.ComparisonType.NOT_EQUALS);
        rule.setThreshold(3);
        assertTrue(rule.evaluate(context));
    }

    private void addMessage(MessageType type, ChatRole role) {
        Message message = Message.builder()
            .id("msg_" + System.nanoTime())
            .messageType(type)
            .role(role)
            .build();
        context.addMessage(message);
    }
}
