package com.backend.softtrainer.simulation.rules.modern;

import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.simulation.rules.FlowRule;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * ⏰ Time Based Rule - Handles timing constraints and time-dependent flow control
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class TimeBasedRule implements FlowRule {
    
    public enum TimeType {
        SESSION_DURATION, LAST_MESSAGE_TIME, RESPONSE_TIME, AVERAGE_RESPONSE_TIME,
        TIME_OF_DAY, DAY_OF_WEEK, INACTIVE_TIME
    }
    
    public enum TimeComparison {
        EQUALS, GREATER_THAN, LESS_THAN, GREATER_EQUAL, LESS_EQUAL, BETWEEN, NOT_EQUALS
    }
    
    @JsonProperty("rule_id")
    private String ruleId;
    
    @JsonProperty("time_type")
    @Builder.Default
    private TimeType timeType = TimeType.SESSION_DURATION;
    
    @JsonProperty("comparison")
    @Builder.Default
    private TimeComparison comparison = TimeComparison.GREATER_THAN;
    
    @JsonProperty("threshold_seconds")
    private Long thresholdSeconds;
    
    @JsonProperty("min_seconds")
    private Long minSeconds;
    
    @JsonProperty("max_seconds")
    private Long maxSeconds;
    
    @JsonProperty("threshold_hours")
    private Integer thresholdHours;
    
    @JsonProperty("threshold_day")
    private Integer thresholdDay;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("warning_enabled")
    @Builder.Default
    private Boolean warningEnabled = false;
    
    @Override
    public boolean evaluate(SimulationContext context) {
        log.debug("⏰ Evaluating TimeBasedRule: {} {} {}", timeType, comparison, thresholdSeconds);
        
        try {
            long actualValue = getTimeValue(context);
            boolean result = performTimeComparison(actualValue);
            
            if (warningEnabled && !result) {
                log.warn("⚠️ Time constraint not met: {} {} {} (actual: {})", 
                    timeType, comparison, thresholdSeconds, actualValue);
            }
            
            log.debug("⏰ Time evaluation: {} {} {} -> {}", actualValue, comparison, thresholdSeconds, result);
            return result;
            
        } catch (Exception e) {
            log.error("❌ Error evaluating time-based rule {}: {}", ruleId, e.getMessage());
            return false;
        }
    }
    
    private long getTimeValue(SimulationContext context) {
        return switch (timeType) {
            case SESSION_DURATION -> context.getDurationSeconds();
            case LAST_MESSAGE_TIME -> getLastMessageTime(context);
            case RESPONSE_TIME -> getLastResponseTime(context);
            case AVERAGE_RESPONSE_TIME -> getAverageResponseTime(context);
            case TIME_OF_DAY -> getCurrentHour();
            case DAY_OF_WEEK -> getCurrentDayOfWeek();
            case INACTIVE_TIME -> getInactiveTime(context);
        };
    }
    
    private long getLastMessageTime(SimulationContext context) {
        if (context.getLastMessage() == null) return 0L;
        
        LocalDateTime lastMessageTime = context.getLastMessage().getTimestamp();
        if (lastMessageTime == null) return 0L;
        
        return ChronoUnit.SECONDS.between(lastMessageTime, LocalDateTime.now());
    }
    
    private long getLastResponseTime(SimulationContext context) {
        if (context.getLastMessage() == null) return 0L;
        
        Long responseTime = context.getLastMessage().getUserResponseTime();
        return responseTime != null ? responseTime / 1000 : 0L;
    }
    
    private long getAverageResponseTime(SimulationContext context) {
        double averageMs = context.getMessageHistory().stream()
            .filter(m -> m.getUserResponseTime() != null)
            .mapToLong(m -> m.getUserResponseTime())
            .average()
            .orElse(0.0);
        
        return (long) (averageMs / 1000);
    }
    
    private long getCurrentHour() {
        return LocalDateTime.now().getHour();
    }
    
    private long getCurrentDayOfWeek() {
        return LocalDateTime.now().getDayOfWeek().getValue();
    }
    
    private long getInactiveTime(SimulationContext context) {
        return context.getMessageHistory().stream()
            .filter(m -> "USER".equals(m.getRole().name()))
            .reduce((first, second) -> second)
            .map(lastUserMessage -> {
                if (lastUserMessage.getTimestamp() != null) {
                    return ChronoUnit.SECONDS.between(lastUserMessage.getTimestamp(), LocalDateTime.now());
                }
                return 0L;
            })
            .orElse(0L);
    }
    
    /**
     * 🔢 Perform time comparison - FIXED VERSION
     */
    private boolean performTimeComparison(long actualValue) {
        return switch (comparison) {
            case EQUALS -> {
                if (timeType == TimeType.TIME_OF_DAY && thresholdHours != null) {
                    yield actualValue == thresholdHours;
                } else if (timeType == TimeType.DAY_OF_WEEK && thresholdDay != null) {
                    yield actualValue == thresholdDay;
                } else {
                    yield actualValue == (thresholdSeconds != null ? thresholdSeconds : 0);
                }
            }
            case NOT_EQUALS -> {
                if (timeType == TimeType.TIME_OF_DAY && thresholdHours != null) {
                    yield actualValue != thresholdHours;
                } else if (timeType == TimeType.DAY_OF_WEEK && thresholdDay != null) {
                    yield actualValue != thresholdDay;
                } else {
                    yield actualValue != (thresholdSeconds != null ? thresholdSeconds : 0);
                }
            }
            case GREATER_THAN -> actualValue > getThresholdValue();
            case LESS_THAN -> actualValue < getThresholdValue();
            case GREATER_EQUAL -> actualValue >= getThresholdValue();
            case LESS_EQUAL -> actualValue <= getThresholdValue();
            case BETWEEN -> actualValue >= minSeconds && actualValue <= maxSeconds;
        };
    }
    
    private long getThresholdValue() {
        return switch (timeType) {
            case TIME_OF_DAY -> thresholdHours != null ? thresholdHours : 0;
            case DAY_OF_WEEK -> thresholdDay != null ? thresholdDay : 1;
            default -> thresholdSeconds != null ? thresholdSeconds : 0;
        };
    }
    
    @Override
    public String getDescription() {
        if (description != null && !description.trim().isEmpty()) {
            return description;
        }
        
        if (comparison == TimeComparison.BETWEEN) {
            return String.format("Check %s between %d and %d seconds", timeType, minSeconds, maxSeconds);
        } else {
            return String.format("Check %s %s %d", timeType, comparison, getThresholdValue());
        }
    }
    
    @Override
    public String getRuleId() {
        return ruleId != null ? ruleId : "time_" + timeType.name().toLowerCase();
    }
    
    @Override
    public int getPriority() {
        return 4;
    }
    
    public static TimeBasedRule sessionTimeout(long timeoutSeconds) {
        return TimeBasedRule.builder()
            .ruleId("session_timeout_" + timeoutSeconds)
            .timeType(TimeType.SESSION_DURATION)
            .comparison(TimeComparison.GREATER_THAN)
            .thresholdSeconds(timeoutSeconds)
            .warningEnabled(true)
            .description("Session timeout after " + timeoutSeconds + " seconds")
            .build();
    }
    
    public static TimeBasedRule responseTimeLimit(long limitSeconds) {
        return TimeBasedRule.builder()
            .ruleId("response_limit_" + limitSeconds)
            .timeType(TimeType.RESPONSE_TIME)
            .comparison(TimeComparison.LESS_EQUAL)
            .thresholdSeconds(limitSeconds)
            .warningEnabled(true)
            .description("Response time must be <= " + limitSeconds + " seconds")
            .build();
    }
    
    public static TimeBasedRule businessHours(int startHour, int endHour) {
        return TimeBasedRule.builder()
            .ruleId("business_hours_" + startHour + "_" + endHour)
            .timeType(TimeType.TIME_OF_DAY)
            .comparison(TimeComparison.BETWEEN)
            .minSeconds((long) startHour)
            .maxSeconds((long) endHour)
            .description(String.format("Active during business hours (%d:00 - %d:00)", startHour, endHour))
            .build();
    }
    
    public static TimeBasedRule inactivityCheck(long maxInactiveSeconds) {
        return TimeBasedRule.builder()
            .ruleId("inactivity_" + maxInactiveSeconds)
            .timeType(TimeType.INACTIVE_TIME)
            .comparison(TimeComparison.LESS_THAN)
            .thresholdSeconds(maxInactiveSeconds)
            .warningEnabled(true)
            .description("Check for inactivity > " + maxInactiveSeconds + " seconds")
            .build();
    }
}
