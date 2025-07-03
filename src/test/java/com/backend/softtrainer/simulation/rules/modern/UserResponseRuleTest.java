package com.backend.softtrainer.simulation.rules.modern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserResponseRule Tests")
class UserResponseRuleTest {
    
    @Test
    @DisplayName("Should create exact match rule")
    void shouldCreateExactMatchRule() {
        UserResponseRule rule = UserResponseRule.exactMatch(5L, List.of(1, 2));
        
        assertEquals("exact_match_5", rule.getRuleId());
        assertEquals(5L, rule.getMessageId());
        assertEquals(List.of(1, 2), rule.getExpectedOptions());
        assertEquals(UserResponseRule.MatchType.EXACT_MATCH, rule.getMatchType());
    }
    
    @Test
    @DisplayName("Should have proper description")
    void shouldHaveProperDescription() {
        UserResponseRule rule = UserResponseRule.exactMatch(10L, List.of(3));
        
        String description = rule.getDescription();
        assertTrue(description.contains("message 10"));
        assertTrue(description.contains("EXACT_MATCH"));
    }
    
    @Test
    @DisplayName("Should have correct priority")
    void shouldHaveCorrectPriority() {
        UserResponseRule rule = UserResponseRule.exactMatch(1L, List.of(1));
        
        assertEquals(5, rule.getPriority());
    }
}
