package com.backend.softtrainer.simulation.monitoring;

import com.backend.softtrainer.simulation.rules.FlowRule;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 📊 Rule Performance Monitor - Tracks execution performance and identifies bottlenecks
 * 
 * Monitors:
 * - Rule execution times
 * - Success/failure rates
 * - Memory usage patterns
 * - Error frequencies
 * - Performance trends
 */
@Service
@Slf4j
public class RulePerformanceMonitor {
    
    private final Map<String, RuleMetrics> ruleMetrics = new ConcurrentHashMap<>();
    private final Map<String, List<ExecutionRecord>> executionHistory = new ConcurrentHashMap<>();
    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    
    /**
     * 📈 Record rule execution
     */
    public void recordExecution(String ruleId, long executionTimeMs, boolean success, String errorMessage) {
        totalExecutions.incrementAndGet();
        
        // Update or create metrics for this rule
        ruleMetrics.compute(ruleId, (key, metrics) -> {
            if (metrics == null) {
                metrics = RuleMetrics.builder()
                    .ruleId(ruleId)
                    .totalExecutions(0L)
                    .totalExecutionTime(0L)
                    .successCount(0L)
                    .errorCount(0L)
                    .minExecutionTime(Long.MAX_VALUE)
                    .maxExecutionTime(0L)
                    .errors(new ArrayList<>())
                    .firstExecuted(LocalDateTime.now())
                    .build();
            }
            
            // Update metrics
            metrics.setTotalExecutions(metrics.getTotalExecutions() + 1);
            metrics.setTotalExecutionTime(metrics.getTotalExecutionTime() + executionTimeMs);
            metrics.setLastExecuted(LocalDateTime.now());
            
            if (success) {
                metrics.setSuccessCount(metrics.getSuccessCount() + 1);
            } else {
                metrics.setErrorCount(metrics.getErrorCount() + 1);
                totalErrors.incrementAndGet();
                
                if (errorMessage != null) {
                    metrics.getErrors().add(ErrorRecord.builder()
                        .timestamp(LocalDateTime.now())
                        .message(errorMessage)
                        .build());
                    
                    // Keep only last 10 errors
                    if (metrics.getErrors().size() > 10) {
                        metrics.getErrors().remove(0);
                    }
                }
            }
            
            // Update execution time bounds
            metrics.setMinExecutionTime(Math.min(metrics.getMinExecutionTime(), executionTimeMs));
            metrics.setMaxExecutionTime(Math.max(metrics.getMaxExecutionTime(), executionTimeMs));
            
            return metrics;
        });
        
        // Record execution history
        ExecutionRecord record = ExecutionRecord.builder()
            .timestamp(LocalDateTime.now())
            .ruleId(ruleId)
            .executionTime(executionTimeMs)
            .success(success)
            .errorMessage(errorMessage)
            .build();
            
        executionHistory.computeIfAbsent(ruleId, k -> new ArrayList<>()).add(record);
        
        // Keep only last 100 executions per rule
        List<ExecutionRecord> history = executionHistory.get(ruleId);
        if (history.size() > 100) {
            history.remove(0);
        }
        
        // Log performance warnings
        checkPerformanceThresholds(ruleId, executionTimeMs, success);
    }
    
    /**
     * ⚠️ Check for performance issues
     */
    private void checkPerformanceThresholds(String ruleId, long executionTime, boolean success) {
        // Warn about slow executions
        if (executionTime > 1000) { // 1 second
            log.warn("🐌 Slow rule execution: {} took {}ms", ruleId, executionTime);
        }
        
        // Warn about frequent failures
        RuleMetrics metrics = ruleMetrics.get(ruleId);
        if (metrics != null && metrics.getTotalExecutions() > 10) {
            double errorRate = (double) metrics.getErrorCount() / metrics.getTotalExecutions();
            if (errorRate > 0.1) { // 10% error rate
                log.warn("⚠️ High error rate for rule {}: {:.1f}%", ruleId, errorRate * 100);
            }
        }
    }
    
    /**
     * 📊 Get performance summary
     */
    public PerformanceSummary getPerformanceSummary() {
        long totalExecs = totalExecutions.get();
        long totalErrs = totalErrors.get();
        double overallErrorRate = totalExecs > 0 ? (double) totalErrs / totalExecs : 0.0;
        
        return PerformanceSummary.builder()
            .totalExecutions(totalExecs)
            .totalErrors(totalErrs)
            .overallErrorRate(overallErrorRate)
            .totalRules(ruleMetrics.size())
            .averageExecutionTime(calculateAverageExecutionTime())
            .slowestRules(getSlowestRules(5))
            .mostErrorProneRules(getMostErrorProneRules(5))
            .recentErrors(getRecentErrors(10))
            .build();
    }
    
    /**
     * 📈 Get metrics for specific rule
     */
    public Optional<RuleMetrics> getRuleMetrics(String ruleId) {
        RuleMetrics metrics = ruleMetrics.get(ruleId);
        if (metrics != null) {
            // Calculate derived metrics
            long totalExecs = metrics.getTotalExecutions();
            metrics.setAverageExecutionTime(
                totalExecs > 0 ? metrics.getTotalExecutionTime() / totalExecs : 0L
            );
            metrics.setSuccessRate(
                totalExecs > 0 ? (double) metrics.getSuccessCount() / totalExecs : 0.0
            );
        }
        return Optional.ofNullable(metrics);
    }
    
    /**
     * 🔄 Start monitoring rule execution
     */
    public ExecutionTimer startTimer(String ruleId) {
        return new ExecutionTimer(ruleId, System.currentTimeMillis());
    }
    
    /**
     * ⏱️ Execution timer for measuring rule performance
     */
    public class ExecutionTimer {
        private final String ruleId;
        private final long startTime;
        
        public ExecutionTimer(String ruleId, long startTime) {
            this.ruleId = ruleId;
            this.startTime = startTime;
        }
        
        public void recordSuccess() {
            long duration = System.currentTimeMillis() - startTime;
            recordExecution(ruleId, duration, true, null);
        }
        
        public void recordError(String errorMessage) {
            long duration = System.currentTimeMillis() - startTime;
            recordExecution(ruleId, duration, false, errorMessage);
        }
        
        public void recordError(Exception exception) {
            recordError(exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }
    
    /**
     * 🐌 Get slowest rules
     */
    private List<RulePerformanceInfo> getSlowestRules(int limit) {
        return ruleMetrics.values().stream()
            .filter(m -> m.getTotalExecutions() > 0)
            .map(m -> RulePerformanceInfo.builder()
                .ruleId(m.getRuleId())
                .averageTime(m.getTotalExecutionTime() / m.getTotalExecutions())
                .totalExecutions(m.getTotalExecutions())
                .successRate((double) m.getSuccessCount() / m.getTotalExecutions())
                .build())
            .sorted(Comparator.comparing(RulePerformanceInfo::getAverageTime).reversed())
            .limit(limit)
            .toList();
    }
    
    /**
     * ⚠️ Get most error-prone rules
     */
    private List<RulePerformanceInfo> getMostErrorProneRules(int limit) {
        return ruleMetrics.values().stream()
            .filter(m -> m.getTotalExecutions() > 5) // Only consider rules with significant usage
            .map(m -> RulePerformanceInfo.builder()
                .ruleId(m.getRuleId())
                .errorRate((double) m.getErrorCount() / m.getTotalExecutions())
                .totalExecutions(m.getTotalExecutions())
                .totalErrors(m.getErrorCount())
                .build())
            .sorted(Comparator.comparing(RulePerformanceInfo::getErrorRate).reversed())
            .limit(limit)
            .toList();
    }
    
    /**
     * 🔥 Get recent errors across all rules
     */
    private List<ErrorRecord> getRecentErrors(int limit) {
        return ruleMetrics.values().stream()
            .flatMap(m -> m.getErrors().stream())
            .sorted(Comparator.comparing(ErrorRecord::getTimestamp).reversed())
            .limit(limit)
            .toList();
    }
    
    /**
     * 📊 Calculate overall average execution time
     */
    private double calculateAverageExecutionTime() {
        return ruleMetrics.values().stream()
            .filter(m -> m.getTotalExecutions() > 0)
            .mapToDouble(m -> (double) m.getTotalExecutionTime() / m.getTotalExecutions())
            .average()
            .orElse(0.0);
    }
    
    /**
     * 🗑️ Clear old performance data
     */
    public void clearOldData(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
        
        executionHistory.values().forEach(history -> 
            history.removeIf(record -> record.getTimestamp().isBefore(cutoff))
        );
        
        ruleMetrics.values().forEach(metrics -> 
            metrics.getErrors().removeIf(error -> error.getTimestamp().isBefore(cutoff))
        );
        
        log.info("🗑️ Cleared performance data older than {} days", daysToKeep);
    }
    
    /**
     * 📊 Data classes for metrics
     */
    
    @Data
    @Builder
    public static class RuleMetrics {
        private String ruleId;
        private Long totalExecutions;
        private Long totalExecutionTime;
        private Long successCount;
        private Long errorCount;
        private Long minExecutionTime;
        private Long maxExecutionTime;
        private LocalDateTime firstExecuted;
        private LocalDateTime lastExecuted;
        private List<ErrorRecord> errors;
        
        // Derived fields
        private Long averageExecutionTime;
        private Double successRate;
    }
    
    @Data
    @Builder
    public static class ExecutionRecord {
        private LocalDateTime timestamp;
        private String ruleId;
        private Long executionTime;
        private Boolean success;
        private String errorMessage;
    }
    
    @Data
    @Builder
    public static class ErrorRecord {
        private LocalDateTime timestamp;
        private String message;
    }
    
    @Data
    @Builder
    public static class PerformanceSummary {
        private Long totalExecutions;
        private Long totalErrors;
        private Double overallErrorRate;
        private Integer totalRules;
        private Double averageExecutionTime;
        private List<RulePerformanceInfo> slowestRules;
        private List<RulePerformanceInfo> mostErrorProneRules;
        private List<ErrorRecord> recentErrors;
    }
    
    @Data
    @Builder
    public static class RulePerformanceInfo {
        private String ruleId;
        private Long averageTime;
        private Double errorRate;
        private Long totalExecutions;
        private Long totalErrors;
        private Double successRate;
    }
} 