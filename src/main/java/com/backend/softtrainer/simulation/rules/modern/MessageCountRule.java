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
 * 💬 Message Count Rule - Tracks conversation progress and triggers flow changes
 * 
 * This rule replaces complex predicate operations like:
 * "message count >= 5 and message count <= 10"
 * 
 * With clear JSON structure:
 * {
 *   "type": "message_count",
 *   "min_count": 5,
 *   "max_count": 10,
 *   "count_type": "TOTAL_MESSAGES"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class MessageCountRule implements FlowRule {
    
    public enum CountType {
        TOTAL_MESSAGES,     // Count all messages in conversation
        USER_MESSAGES,      // Count only user messages
        SYSTEM_MESSAGES,    // Count only system/bot messages
        QUESTION_MESSAGES,  // Count only question-type messages
        ANSWER_MESSAGES     // Count only answer-type messages
    }
    
    public enum ComparisonType {
        EQUALS,             // Exact count match
        GREATER_THAN,       // Count > threshold
        LESS_THAN,          // Count < threshold
        GREATER_EQUAL,      // Count >= threshold
        LESS_EQUAL,         // Count <= threshold
        BETWEEN,            // Count within range [min, max]
        NOT_EQUALS          // Count != threshold
    }
    
    @JsonProperty("rule_id")
    private String ruleId;
    
    @JsonProperty("count_type")
    @Builder.Default
    private CountType countType = CountType.TOTAL_MESSAGES;
    
    @JsonProperty("comparison_type")
    @Builder.Default
    private ComparisonType comparisonType = ComparisonType.GREATER_EQUAL;
    
    @JsonProperty("threshold")
    private Integer threshold;
    
    @JsonProperty("min_count")
    private Integer minCount;
    
    @JsonProperty("max_count")
    private Integer maxCount;
    
    @JsonProperty("description")
    private String description;
    
    @Override
    public boolean evaluate(SimulationContext context) {
        log.debug("💬 Evaluating MessageCount rule: {} {} {}", 
            countType, comparisonType, threshold);
        
        try {
            int actualCount = getMessageCount(context);
            boolean result = performComparison(actualCount);
            
            log.debug("💬 Message count evaluation: {} {} {} -> {}", 
                actualCount, comparisonType, threshold, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Error evaluating message count rule {}: {}", 
                ruleId, e.getMessage());
            return false;
        }
    }
    
    /**
     * 📊 Get message count based on count type
     */
    private int getMessageCount(SimulationContext context) {
        return switch (countType) {
            case TOTAL_MESSAGES -> context.getMessageCount();
            case USER_MESSAGES -> getUserMessageCount(context);
            case SYSTEM_MESSAGES -> getSystemMessageCount(context);
            case QUESTION_MESSAGES -> getQuestionMessageCount(context);
            case ANSWER_MESSAGES -> getAnswerMessageCount(context);
        };
    }
    
    /**
     * 👤 Count user messages
     */
    private int getUserMessageCount(SimulationContext context) {
        return (int) context.getMessageHistory().stream()
            .filter(m -> "USER".equals(m.getRole().name()))
            .count();
    }
    
    /**
     * 🤖 Count system/bot messages
     */
    private int getSystemMessageCount(SimulationContext context) {
        return (int) context.getMessageHistory().stream()
            .filter(m -> !"USER".equals(m.getRole().name()))
            .count();
    }
    
    /**
     * ❓ Count question-type messages
     */
    private int getQuestionMessageCount(SimulationContext context) {
        return (int) context.getMessageHistory().stream()
            .filter(m -> m.getMessageType().name().contains("QUESTION"))
            .count();
    }
    
    /**
     * ✅ Count answer-type messages
     */
    private int getAnswerMessageCount(SimulationContext context) {
        return (int) context.getMessageHistory().stream()
            .filter(m -> m.getMessageType().name().contains("ANSWER"))
            .count();
    }
    
    /**
     * 🔢 Perform the comparison operation
     */
    private boolean performComparison(int actualCount) {
        return switch (comparisonType) {
            case EQUALS -> actualCount == threshold;
            case GREATER_THAN -> actualCount > threshold;
            case LESS_THAN -> actualCount < threshold;
            case GREATER_EQUAL -> actualCount >= threshold;
            case LESS_EQUAL -> actualCount <= threshold;
            case NOT_EQUALS -> actualCount != threshold;
            case BETWEEN -> actualCount >= minCount && actualCount <= maxCount;
        };
    }
    
    @Override
    public String getDescription() {
        if (description != null && !description.trim().isEmpty()) {
            return description;
        }
        
        if (comparisonType == ComparisonType.BETWEEN) {
            return String.format("Check %s between %d and %d", 
                countType, minCount, maxCount);
        } else {
            return String.format("Check %s %s %d", 
                countType, comparisonType, threshold);
        }
    }
    
    @Override
    public String getRuleId() {
        return ruleId != null ? ruleId : "message_count_" + countType.name().toLowerCase();
    }
    
    @Override
    public int getPriority() {
        return 3; // Lower priority for count-based rules
    }
    
    /**
     * 🏗️ Builder methods for common patterns
     */
    public static MessageCountRule minTotalMessages(int minCount) {
        return MessageCountRule.builder()
            .ruleId("min_total_" + minCount)
            .countType(CountType.TOTAL_MESSAGES)
            .comparisonType(ComparisonType.GREATER_EQUAL)
            .threshold(minCount)
            .description("Require at least " + minCount + " total messages")
            .build();
    }
    
    public static MessageCountRule maxUserMessages(int maxCount) {
        return MessageCountRule.builder()
            .ruleId("max_user_" + maxCount)
            .countType(CountType.USER_MESSAGES)
            .comparisonType(ComparisonType.LESS_EQUAL)
            .threshold(maxCount)
            .description("Limit user messages to " + maxCount)
            .build();
    }
    
    public static MessageCountRule betweenCounts(CountType type, int min, int max) {
        return MessageCountRule.builder()
            .ruleId("between_" + type.name().toLowerCase() + "_" + min + "_" + max)
            .countType(type)
            .comparisonType(ComparisonType.BETWEEN)
            .minCount(min)
            .maxCount(max)
            .description(String.format("Check %s between %d and %d", type, min, max))
            .build();
    }
}
