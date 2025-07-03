package com.backend.softtrainer.simulation.rules.modern;

import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.enums.ChatRole;
import com.backend.softtrainer.entities.enums.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TimeBasedRule Tests")
class TimeBasedRuleTest {
    
    private SimulationContext context;
    
    @BeforeEach
    void setUp() {
        context = SimulationContext.builder()
            .chatId(1L)
            .hearts(5.0)
            .startTime(System.currentTimeMillis() - 5000) // Started 5 seconds ago
            .build();
    }
    
    @Test
    @DisplayName("Should create session timeout rule")
    void shouldCreateSessionTimeoutRule() {
        TimeBasedRule rule = TimeBasedRule.sessionTimeout(300L);
        
        assertEquals("session_timeout_300", rule.getRuleId());
        assertEquals(TimeBasedRule.TimeType.SESSION_DURATION, rule.getTimeType());
        assertEquals(TimeBasedRule.TimeComparison.GREATER_THAN, rule.getComparison());
        assertEquals(300L, rule.getThresholdSeconds());
        assertTrue(rule.getWarningEnabled());
    }
    
    @Test
    @DisplayName("Should create response time limit rule")
    void shouldCreateResponseTimeLimitRule() {
        TimeBasedRule rule = TimeBasedRule.responseTimeLimit(30L);
        
        assertEquals("response_limit_30", rule.getRuleId());
        assertEquals(TimeBasedRule.TimeType.RESPONSE_TIME, rule.getTimeType());
        assertEquals(TimeBasedRule.TimeComparison.LESS_EQUAL, rule.getComparison());
        assertEquals(30L, rule.getThresholdSeconds());
    }
    
    @Test
    @DisplayName("Should create business hours rule")
    void shouldCreateBusinessHoursRule() {
        TimeBasedRule rule = TimeBasedRule.businessHours(9, 17);
        
        assertEquals("business_hours_9_17", rule.getRuleId());
        assertEquals(TimeBasedRule.TimeType.TIME_OF_DAY, rule.getTimeType());
        assertEquals(TimeBasedRule.TimeComparison.BETWEEN, rule.getComparison());
        assertEquals(9L, rule.getMinSeconds());
        assertEquals(17L, rule.getMaxSeconds());
    }
    
    @Test
    @DisplayName("Should create inactivity check rule")
    void shouldCreateInactivityCheckRule() {
        TimeBasedRule rule = TimeBasedRule.inactivityCheck(120L);
        
        assertEquals("inactivity_120", rule.getRuleId());
        assertEquals(TimeBasedRule.TimeType.INACTIVE_TIME, rule.getTimeType());
        assertEquals(TimeBasedRule.TimeComparison.LESS_THAN, rule.getComparison());
        assertEquals(120L, rule.getThresholdSeconds());
    }
    
    @Test
    @DisplayName("Should evaluate session duration correctly")
    void shouldEvaluateSessionDuration() {
        TimeBasedRule rule = TimeBasedRule.builder()
            .timeType(TimeBasedRule.TimeType.SESSION_DURATION)
            .comparison(TimeBasedRule.TimeComparison.GREATER_THAN)
            .thresholdSeconds(3L) // Should pass since session started 5 seconds ago
            .build();
        
        assertTrue(rule.evaluate(context));
        
        // Test with higher threshold
        rule.setThresholdSeconds(10L);
        assertFalse(rule.evaluate(context));
    }
    
    @Test
    @DisplayName("Should evaluate response time limits")
    void shouldEvaluateResponseTimeLimits() {
        // Add a message with response time
        Message message = Message.builder()
            .id("msg_1")
            .messageType(MessageType.SINGLE_CHOICE_ANSWER)
            .role(ChatRole.USER)
            .userResponseTime(15000L) // 15 seconds in milliseconds
            .timestamp(LocalDateTime.now())
            .build();
        context.addMessage(message);
        
        TimeBasedRule rule = TimeBasedRule.builder()
            .timeType(TimeBasedRule.TimeType.RESPONSE_TIME)
            .comparison(TimeBasedRule.TimeComparison.LESS_THAN)
            .thresholdSeconds(20L) // 20 seconds threshold
            .build();
        
        assertTrue(rule.evaluate(context)); // 15 < 20
        
        rule.setThresholdSeconds(10L); // 10 seconds threshold
        assertFalse(rule.evaluate(context)); // 15 > 10
    }
    
    @Test
    @DisplayName("Should calculate average response time")
    void shouldCalculateAverageResponseTime() {
        // Add messages with different response times
        addMessageWithResponseTime(10000L); // 10 seconds
        addMessageWithResponseTime(20000L); // 20 seconds  
        addMessageWithResponseTime(30000L); // 30 seconds
        // Average = 20 seconds
        
        TimeBasedRule rule = TimeBasedRule.builder()
            .timeType(TimeBasedRule.TimeType.AVERAGE_RESPONSE_TIME)
            .comparison(TimeBasedRule.TimeComparison.EQUALS)
            .thresholdSeconds(20L)
            .build();
        
        assertTrue(rule.evaluate(context));
    }
    
    @Test
    @DisplayName("Should handle between comparisons")
    void shouldHandleBetweenComparisons() {
        TimeBasedRule rule = TimeBasedRule.builder()
            .timeType(TimeBasedRule.TimeType.SESSION_DURATION)
            .comparison(TimeBasedRule.TimeComparison.BETWEEN)
            .minSeconds(3L)
            .maxSeconds(10L)
            .build();
        
        assertTrue(rule.evaluate(context)); // Session ~5 seconds, within range
        
        rule.setMinSeconds(6L);
        rule.setMaxSeconds(10L);
        assertFalse(rule.evaluate(context)); // Session ~5 seconds, below range
    }
    
    @Test
    @DisplayName("Should handle time of day checks")
    void shouldHandleTimeOfDayChecks() {
        int currentHour = LocalDateTime.now().getHour();
        
        TimeBasedRule rule = TimeBasedRule.builder()
            .timeType(TimeBasedRule.TimeType.TIME_OF_DAY)
            .comparison(TimeBasedRule.TimeComparison.EQUALS)
            .thresholdHours(currentHour)
            .build();
        
        assertTrue(rule.evaluate(context));
        
        rule.setThresholdHours((currentHour + 1) % 24);
        assertFalse(rule.evaluate(context));
    }
    
    @Test
    @DisplayName("Should handle day of week checks")
    void shouldHandleDayOfWeekChecks() {
        int currentDay = LocalDateTime.now().getDayOfWeek().getValue();
        
        TimeBasedRule rule = TimeBasedRule.builder()
            .timeType(TimeBasedRule.TimeType.DAY_OF_WEEK)
            .comparison(TimeBasedRule.TimeComparison.EQUALS)
            .thresholdDay(currentDay)
            .build();
        
        assertTrue(rule.evaluate(context));
        
        rule.setThresholdDay((currentDay % 7) + 1);
        assertFalse(rule.evaluate(context));
    }
    
    @Test
    @DisplayName("Should have correct priority")
    void shouldHaveCorrectPriority() {
        TimeBasedRule rule = TimeBasedRule.sessionTimeout(300L);
        assertEquals(4, rule.getPriority());
    }
    
    @Test
    @DisplayName("Should generate proper descriptions")
    void shouldGenerateProperDescriptions() {
        TimeBasedRule rule1 = TimeBasedRule.sessionTimeout(300L);
        assertTrue(rule1.getDescription().contains("timeout after 300 seconds"));
        
        TimeBasedRule rule2 = TimeBasedRule.businessHours(9, 17);
        assertTrue(rule2.getDescription().contains("9:00 - 17:00"));
        
        TimeBasedRule rule3 = TimeBasedRule.responseTimeLimit(30L);
        assertTrue(rule3.getDescription().contains("<= 30 seconds"));
    }
    
    @Test
    @DisplayName("Should handle null safety")
    void shouldHandleNullSafety() {
        // Context with no messages
        SimulationContext emptyContext = SimulationContext.builder()
            .chatId(2L)
            .hearts(5.0)
            .build();
        
        TimeBasedRule rule = TimeBasedRule.builder()
            .timeType(TimeBasedRule.TimeType.RESPONSE_TIME)
            .comparison(TimeBasedRule.TimeComparison.GREATER_THAN)
            .thresholdSeconds(0L)
            .build();
        
        // Should not throw exception and return false (0 > 0 is false)
        assertFalse(rule.evaluate(emptyContext));
    }
    
    private void addMessageWithResponseTime(Long responseTimeMs) {
        Message message = Message.builder()
            .id("msg_" + System.nanoTime())
            .messageType(MessageType.SINGLE_CHOICE_ANSWER)
            .role(ChatRole.USER)
            .userResponseTime(responseTimeMs)
            .timestamp(LocalDateTime.now())
            .build();
        context.addMessage(message);
    }
}
