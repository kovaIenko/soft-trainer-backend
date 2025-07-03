package com.backend.softtrainer.simulation.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RulePerformanceMonitor Tests")
class RulePerformanceMonitorTest {
    
    private RulePerformanceMonitor monitor;
    
    @BeforeEach
    void setUp() {
        monitor = new RulePerformanceMonitor();
    }
    
    @Test
    @DisplayName("Should record successful rule execution")
    void shouldRecordSuccessfulExecution() {
        String ruleId = "test_rule_1";
        
        monitor.recordExecution(ruleId, 100L, true, null);
        
        Optional<RulePerformanceMonitor.RuleMetrics> metrics = monitor.getRuleMetrics(ruleId);
        assertTrue(metrics.isPresent());
        
        RulePerformanceMonitor.RuleMetrics m = metrics.get();
        assertEquals(1L, m.getTotalExecutions());
        assertEquals(1L, m.getSuccessCount());
        assertEquals(0L, m.getErrorCount());
        assertEquals(100L, m.getTotalExecutionTime());
        assertEquals(100L, m.getAverageExecutionTime());
        assertEquals(1.0, m.getSuccessRate());
    }
    
    @Test
    @DisplayName("Should record failed rule execution")
    void shouldRecordFailedExecution() {
        String ruleId = "test_rule_2";
        String errorMessage = "Test error";
        
        monitor.recordExecution(ruleId, 50L, false, errorMessage);
        
        Optional<RulePerformanceMonitor.RuleMetrics> metrics = monitor.getRuleMetrics(ruleId);
        assertTrue(metrics.isPresent());
        
        RulePerformanceMonitor.RuleMetrics m = metrics.get();
        assertEquals(1L, m.getTotalExecutions());
        assertEquals(0L, m.getSuccessCount());
        assertEquals(1L, m.getErrorCount());
        assertEquals(0.0, m.getSuccessRate());
        assertEquals(1, m.getErrors().size());
        assertEquals(errorMessage, m.getErrors().get(0).getMessage());
    }
    
    @Test
    @DisplayName("Should track execution time bounds")
    void shouldTrackExecutionTimeBounds() {
        String ruleId = "test_rule_3";
        
        monitor.recordExecution(ruleId, 50L, true, null);
        monitor.recordExecution(ruleId, 200L, true, null);
        monitor.recordExecution(ruleId, 100L, true, null);
        
        Optional<RulePerformanceMonitor.RuleMetrics> metrics = monitor.getRuleMetrics(ruleId);
        assertTrue(metrics.isPresent());
        
        RulePerformanceMonitor.RuleMetrics m = metrics.get();
        assertEquals(50L, m.getMinExecutionTime());
        assertEquals(200L, m.getMaxExecutionTime());
        assertEquals(116L, m.getAverageExecutionTime()); // (50+200+100)/3 = 116.67
    }
    
    @Test
    @DisplayName("Should provide performance summary")
    void shouldProvidePerformanceSummary() {
        monitor.recordExecution("rule1", 100L, true, null);
        monitor.recordExecution("rule2", 200L, false, "Error");
        monitor.recordExecution("rule1", 150L, true, null);
        
        RulePerformanceMonitor.PerformanceSummary summary = monitor.getPerformanceSummary();
        
        assertEquals(3L, summary.getTotalExecutions());
        assertEquals(1L, summary.getTotalErrors());
        assertEquals(1.0/3.0, summary.getOverallErrorRate(), 0.01);
        assertEquals(2, summary.getTotalRules());
        assertTrue(summary.getAverageExecutionTime() > 0);
        assertNotNull(summary.getSlowestRules());
        assertNotNull(summary.getMostErrorProneRules());
        assertNotNull(summary.getRecentErrors());
    }
    
    @Test
    @DisplayName("Should use execution timer correctly")
    void shouldUseExecutionTimer() {
        String ruleId = "timer_test_rule";
        
        RulePerformanceMonitor.ExecutionTimer timer = monitor.startTimer(ruleId);
        
        // Simulate some work
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        timer.recordSuccess();
        
        Optional<RulePerformanceMonitor.RuleMetrics> metrics = monitor.getRuleMetrics(ruleId);
        assertTrue(metrics.isPresent());
        assertEquals(1L, metrics.get().getTotalExecutions());
        assertEquals(1L, metrics.get().getSuccessCount());
        assertTrue(metrics.get().getTotalExecutionTime() >= 10L);
    }
    
    @Test
    @DisplayName("Should handle timer with error")
    void shouldHandleTimerWithError() {
        String ruleId = "timer_error_rule";
        
        RulePerformanceMonitor.ExecutionTimer timer = monitor.startTimer(ruleId);
        Exception testException = new RuntimeException("Test exception");
        timer.recordError(testException);
        
        Optional<RulePerformanceMonitor.RuleMetrics> metrics = monitor.getRuleMetrics(ruleId);
        assertTrue(metrics.isPresent());
        assertEquals(1L, metrics.get().getErrorCount());
        assertTrue(metrics.get().getErrors().get(0).getMessage().contains("RuntimeException"));
    }
    
    @Test
    @DisplayName("Should limit error history")
    void shouldLimitErrorHistory() {
        String ruleId = "error_limit_rule";
        
        // Record more than 10 errors
        for (int i = 0; i < 15; i++) {
            monitor.recordExecution(ruleId, 10L, false, "Error " + i);
        }
        
        Optional<RulePerformanceMonitor.RuleMetrics> metrics = monitor.getRuleMetrics(ruleId);
        assertTrue(metrics.isPresent());
        
        // Should keep only last 10 errors
        assertEquals(10, metrics.get().getErrors().size());
        assertEquals(15L, metrics.get().getErrorCount());
    }
    
    @Test
    @DisplayName("Should clear old data")
    void shouldClearOldData() {
        monitor.recordExecution("test_rule", 100L, true, null);
        
        assertDoesNotThrow(() -> monitor.clearOldData(30));
        
        // Since we just recorded data, it shouldn't be cleared
        Optional<RulePerformanceMonitor.RuleMetrics> metrics = monitor.getRuleMetrics("test_rule");
        assertTrue(metrics.isPresent());
    }
    
    @Test
    @DisplayName("Should handle non-existent rule metrics")
    void shouldHandleNonExistentRule() {
        Optional<RulePerformanceMonitor.RuleMetrics> metrics = monitor.getRuleMetrics("non_existent_rule");
        assertTrue(metrics.isEmpty());
    }
} 