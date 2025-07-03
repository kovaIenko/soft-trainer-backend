package com.backend.softtrainer.simulation.rules;

import com.backend.softtrainer.simulation.context.SimulationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Rule engine for evaluating flow control rules
 * Replaces the opaque showPredicate system with transparent, testable rules
 */
@Service
@Slf4j
public class RuleEngine {
    
    /**
     * Evaluate a list of rules using AND logic
     * All rules must pass for the result to be true
     * 
     * @param rules List of rules to evaluate
     * @param context Simulation context
     * @return true if all rules pass
     */
    public boolean evaluateAll(List<FlowRule> rules, SimulationContext context) {
        if (rules == null || rules.isEmpty()) {
            log.debug("No rules provided, defaulting to true");
            return true;
        }
        
        for (FlowRule rule : rules) {
            try {
                boolean result = rule.evaluate(context);
                log.debug("Rule '{}' ({}) evaluated to: {}", 
                    rule.getRuleId(), rule.getDescription(), result);
                
                if (!result) {
                    log.debug("Rule '{}' failed, short-circuiting evaluation", rule.getRuleId());
                    return false;
                }
            } catch (Exception e) {
                log.error("Error evaluating rule '{}': {}", rule.getRuleId(), e.getMessage(), e);
                return false; // Fail safe
            }
        }
        
        log.debug("All {} rules passed", rules.size());
        return true;
    }
    
    /**
     * Evaluate a list of rules using OR logic
     * At least one rule must pass for the result to be true
     * 
     * @param rules List of rules to evaluate
     * @param context Simulation context
     * @return true if any rule passes
     */
    public boolean evaluateAny(List<FlowRule> rules, SimulationContext context) {
        if (rules == null || rules.isEmpty()) {
            log.debug("No rules provided, defaulting to false for OR evaluation");
            return false;
        }
        
        for (FlowRule rule : rules) {
            try {
                boolean result = rule.evaluate(context);
                log.debug("Rule '{}' ({}) evaluated to: {}", 
                    rule.getRuleId(), rule.getDescription(), result);
                
                if (result) {
                    log.debug("Rule '{}' passed, short-circuiting OR evaluation", rule.getRuleId());
                    return true;
                }
            } catch (Exception e) {
                log.error("Error evaluating rule '{}': {}", rule.getRuleId(), e.getMessage(), e);
                // Continue with other rules in OR evaluation
            }
        }
        
        log.debug("No rules passed out of {}", rules.size());
        return false;
    }
    
    /**
     * Find the first rule that passes
     * 
     * @param rules List of rules to evaluate
     * @param context Simulation context
     * @return Optional containing the first passing rule
     */
    public Optional<FlowRule> findFirstPassing(List<FlowRule> rules, SimulationContext context) {
        if (rules == null || rules.isEmpty()) {
            return Optional.empty();
        }
        
        // Sort by priority (higher first)
        List<FlowRule> sortedRules = rules.stream()
            .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()))
            .toList();
        
        for (FlowRule rule : sortedRules) {
            try {
                if (rule.evaluate(context)) {
                    log.debug("First passing rule: '{}' ({})", rule.getRuleId(), rule.getDescription());
                    return Optional.of(rule);
                }
            } catch (Exception e) {
                log.error("Error evaluating rule '{}': {}", rule.getRuleId(), e.getMessage(), e);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Evaluate a single rule with error handling
     * 
     * @param rule Rule to evaluate
     * @param context Simulation context
     * @return true if rule passes, false if fails or throws exception
     */
    public boolean evaluateSafe(FlowRule rule, SimulationContext context) {
        try {
            boolean result = rule.evaluate(context);
            log.debug("Rule '{}' evaluated to: {}", rule.getRuleId(), result);
            return result;
        } catch (Exception e) {
            log.error("Error evaluating rule '{}': {}", rule.getRuleId(), e.getMessage(), e);
            return false;
        }
    }
} 