package com.backend.softtrainer.simulation.rules;

import com.backend.softtrainer.entities.flow.FlowRule;
import com.backend.softtrainer.entities.flow.RuleAction;
import com.backend.softtrainer.entities.flow.RuleCondition;
import com.backend.softtrainer.simulation.context.SimulationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 🔧 Modern Rule Evaluator - Rule Processing Engine
 * 
 * Evaluates modern simulation rules, conditions, and executes actions
 * for the rule-based simulation format.
 */
@Service
@Slf4j
public class ModernRuleEvaluator {
    
    /**
     * 🎯 Evaluate Rule Conditions
     * 
     * Checks if all conditions for a rule are met
     */
    public boolean evaluateRule(FlowRule rule, SimulationContext context, String selectedOptionId, String userAnswer) {
        log.debug("🔍 Evaluating rule type: {}", rule.getType());
        
        switch (rule.getType()) {
            case "ALWAYS_SHOW":
                return true;
                
            case "DEPENDS_ON_PREVIOUS":
                return evaluatePreviousDependency(rule, context);
                
            case "CONDITIONAL_BRANCHING":
                return evaluateConditionalBranching(rule, context, selectedOptionId);
                
            case "ANSWER_QUALITY_RULE":
                return evaluateAnswerQuality(rule, context, userAnswer);
                
            case "FINAL_EVALUATION":
                return evaluateFinalConditions(rule, context);
                
            default:
                log.warn("⚠️ Unknown rule type: {}", rule.getType());
                return false;
        }
    }
    
    /**
     * ⚡ Execute Rule Actions
     * 
     * Executes all actions associated with a rule or condition
     */
    public void executeActions(List<RuleAction> actions, SimulationContext context) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        
        log.debug("⚡ Executing {} actions", actions.size());
        
        for (RuleAction action : actions) {
            executeAction(action, context);
        }
    }
    
    /**
     * 🎯 Execute Single Action
     */
    private void executeAction(RuleAction action, SimulationContext context) {
        log.debug("⚡ Executing action: {} for key: {}", action.getType(), action.getKey());
        
        switch (action.getType()) {
            case "INCREASE_HYPERPARAMETER":
                increaseHyperparameter(action.getKey(), action.getValue(), context);
                break;
                
            case "DECREASE_HYPERPARAMETER":
                decreaseHyperparameter(action.getKey(), action.getValue(), context);
                break;
                
            case "SET_HYPERPARAMETER":
                setHyperparameter(action.getKey(), action.getValue(), context);
                break;
                
            default:
                log.warn("⚠️ Unknown action type: {}", action.getType());
        }
    }
    
    /**
     * 📈 Increase Hyperparameter
     */
    private void increaseHyperparameter(String key, Integer value, SimulationContext context) {
        if (key == null || value == null) {
            log.warn("⚠️ Invalid hyperparameter increase: key={}, value={}", key, value);
            return;
        }
        
        Double currentValue = context.getHyperParameter(key);
        Double newValue = currentValue + value.doubleValue();
        
        context.setHyperParameter(key, newValue);
        
        log.info("📈 Increased hyperparameter {} from {} to {} (+{})", key, currentValue, newValue, value);
    }
    
    /**
     * 📉 Decrease Hyperparameter
     */
    private void decreaseHyperparameter(String key, Integer value, SimulationContext context) {
        if (key == null || value == null) {
            log.warn("⚠️ Invalid hyperparameter decrease: key={}, value={}", key, value);
            return;
        }
        
        Double currentValue = context.getHyperParameter(key);
        Double newValue = Math.max(0.0, currentValue - value.doubleValue()); // Don't go below 0
        
        context.setHyperParameter(key, newValue);
        
        log.info("📉 Decreased hyperparameter {} from {} to {} (-{})", key, currentValue, newValue, value);
    }
    
    /**
     * 📊 Set Hyperparameter
     */
    private void setHyperparameter(String key, Integer value, SimulationContext context) {
        if (key == null || value == null) {
            log.warn("⚠️ Invalid hyperparameter set: key={}, value={}", key, value);
            return;
        }
        
        Double oldValue = context.getHyperParameter(key);
        context.setHyperParameter(key, value.doubleValue());
        
        log.info("📊 Set hyperparameter {} from {} to {}", key, oldValue, value);
    }
    
    /**
     * 🔗 Evaluate Previous Dependency
     */
    private boolean evaluatePreviousDependency(FlowRule rule, SimulationContext context) {
        // For DEPENDS_ON_PREVIOUS, we just need to check if the previous message was shown
        // In practice, this is usually handled by the flow logic
        log.debug("✅ Previous dependency rule always true for flow logic");
        return true;
    }
    
    /**
     * 🔀 Evaluate Conditional Branching
     */
    private boolean evaluateConditionalBranching(FlowRule rule, SimulationContext context, String selectedOptionId) {
        if (rule.getConditions() == null || selectedOptionId == null) {
            return false;
        }
        
        for (RuleCondition condition : rule.getConditions()) {
            if ("OPTION_SELECTED".equals(condition.getType()) && 
                selectedOptionId.equals(condition.getOptionId())) {
                
                // Execute actions for this condition
                executeActions(condition.getActions(), context);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 📝 Evaluate Answer Quality
     */
    private boolean evaluateAnswerQuality(FlowRule rule, SimulationContext context, String userAnswer) {
        // For now, assume any non-empty answer is good quality
        // In the future, this could use AI to evaluate answer quality
        if (userAnswer != null && !userAnswer.trim().isEmpty()) {
            executeActions(rule.getActions(), context);
            return true;
        }
        
        return false;
    }
    
    /**
     * 🏁 Evaluate Final Conditions
     */
    private boolean evaluateFinalConditions(FlowRule rule, SimulationContext context) {
        if (rule.getConditions() == null) {
            return true;
        }
        
        for (RuleCondition condition : rule.getConditions()) {
            if ("HYPERPARAMETER_THRESHOLD".equals(condition.getType())) {
                Double currentValue = context.getHyperParameter(condition.getKey());
                
                if (condition.getMinValue() != null && currentValue < condition.getMinValue().doubleValue()) {
                    log.debug("❌ Hyperparameter {} ({}) below minimum threshold ({})", 
                            condition.getKey(), currentValue, condition.getMinValue());
                    return false;
                }
                
                if (condition.getMaxValue() != null && currentValue > condition.getMaxValue().doubleValue()) {
                    log.debug("❌ Hyperparameter {} ({}) above maximum threshold ({})", 
                            condition.getKey(), currentValue, condition.getMaxValue());
                    return false;
                }
            }
        }
        
        log.debug("✅ All final conditions met");
        return true;
    }
} 