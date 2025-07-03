package com.backend.softtrainer.simulation.flow;

import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.entities.flow.EnhancedFlowNode;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.simulation.rules.FlowRule;
import com.backend.softtrainer.simulation.rules.RuleEngine;
import com.backend.softtrainer.services.UserHyperParameterService;
import com.oruel.conditionscript.libs.MessageManagerLib;
import com.oruel.conditionscript.script.ConditionScriptRunnerKt;
import com.oruel.scriptforge.Runner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 🔧 Rule Unifier - Bridges legacy show_predicate and modern rule systems
 * 
 * This is the compatibility layer that allows both old and new rule systems
 * to coexist and be evaluated consistently.
 */
@Service
@Slf4j
public class RuleUnifier {
    
    private final RuleEngine ruleEngine;
    private final UserHyperParameterService userHyperParameterService;
    private final Runner conditionScriptRunner = ConditionScriptRunnerKt.ConditionScriptRunner();
    private final ObjectMapper objectMapper;
    
    public RuleUnifier(RuleEngine ruleEngine, 
                      UserHyperParameterService userHyperParameterService,
                      ObjectMapper objectMapper) {
        this.ruleEngine = ruleEngine;
        this.userHyperParameterService = userHyperParameterService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 🎯 Main evaluation method - handles both legacy and modern nodes
     */
    public boolean evaluateNode(SimulationContext context, Object node) {
        if (node instanceof FlowNode) {
            return evaluateLegacyNode(context, (FlowNode) node);
        } else if (node instanceof EnhancedFlowNode) {
            return evaluateModernNode(context, (EnhancedFlowNode) node);
        } else {
            log.warn("⚠️ Unknown node type: {}", node.getClass().getSimpleName());
            return false;
        }
    }
    
    /**
     * 📜 Evaluate legacy FlowNode with show_predicate
     */
    private boolean evaluateLegacyNode(SimulationContext context, FlowNode flowNode) {
        String predicate = flowNode.getShowPredicate();
        
        // Empty predicate means always show
        if (predicate == null || predicate.trim().isEmpty()) {
            log.debug("✅ Empty predicate for node {}, defaulting to true", flowNode.getOrderNumber());
            return true;
        }
        
        try {
            // Set up the legacy message manager lib
            var messageManagerLib = new MessageManagerLib(
                (Long orderNumber) -> getLegacyMessage(context, orderNumber),
                (String key) -> userHyperParameterService.getOrCreateUserHyperParameter(
                    context.getChatId(), key),
                (String key, Double value) -> userHyperParameterService.update(
                    context.getChatId(), key, value)
            );
            
            // Reset and load libs
            conditionScriptRunner.resetLibs();
            conditionScriptRunner.loadLib(messageManagerLib.getLib());
            
            // Run the predicate
            boolean result = conditionScriptRunner.runPredicate(predicate);
            
            log.debug("📊 Legacy predicate '{}' for node {} evaluated to: {}", 
                predicate, flowNode.getOrderNumber(), result);
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Error evaluating legacy predicate for node {}: {}", 
                flowNode.getOrderNumber(), e.getMessage());
            return false; // Fail safe
        }
    }
    
    /**
     * 🚀 Evaluate modern EnhancedFlowNode with structured rules
     */
    private boolean evaluateModernNode(SimulationContext context, EnhancedFlowNode flowNode) {
        try {
            // Check if node has legacy predicate as fallback
            if (flowNode.hasComplexPredicate()) {
                log.debug("🔄 EnhancedFlowNode {} has legacy predicate, using legacy evaluation", 
                    flowNode.getMessageId());
                return evaluateLegacyPredicate(context, flowNode.getShowPredicate());
            }
            
            // Parse modern flow rules from JSON
            if (flowNode.getFlowRules() != null) {
                List<FlowRule> rules = parseFlowRules(flowNode.getFlowRules().toString());
                boolean result = ruleEngine.evaluateAll(rules, context);
                
                log.debug("🎯 Modern rules for node {} evaluated to: {}", 
                    flowNode.getMessageId(), result);
                
                return result;
            }
            
            // No rules means always show
            log.debug("✅ No rules for EnhancedFlowNode {}, defaulting to true", 
                flowNode.getMessageId());
            return true;
            
        } catch (Exception e) {
            log.error("❌ Error evaluating modern node {}: {}", 
                flowNode.getMessageId(), e.getMessage());
            return false; // Fail safe
        }
    }
    
    /**
     * 📜 Helper method to evaluate legacy predicate string
     */
    private boolean evaluateLegacyPredicate(SimulationContext context, String predicate) {
        if (predicate == null || predicate.trim().isEmpty()) {
            return true;
        }
        
        try {
            var messageManagerLib = new MessageManagerLib(
                (Long orderNumber) -> getLegacyMessage(context, orderNumber),
                (String key) -> userHyperParameterService.getOrCreateUserHyperParameter(
                    context.getChatId(), key),
                (String key, Double value) -> userHyperParameterService.update(
                    context.getChatId(), key, value)
            );
            
            conditionScriptRunner.resetLibs();
            conditionScriptRunner.loadLib(messageManagerLib.getLib());
            
            return conditionScriptRunner.runPredicate(predicate);
            
        } catch (Exception e) {
            log.error("❌ Error evaluating legacy predicate: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 🔧 Parse flow rules from JSON
     */
    private List<FlowRule> parseFlowRules(String flowRulesJson) {
        try {
            // TODO: Implement proper JSON to FlowRule parsing
            // For now, return empty list
            return List.of();
        } catch (Exception e) {
            log.error("❌ Error parsing flow rules JSON: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * 💬 Get legacy message for predicate evaluation
     */
    private com.oruel.conditionscript.Message getLegacyMessage(SimulationContext context, Long orderNumber) {
        try {
            // TODO: Implement proper message retrieval
            // This should use the existing message service logic
            log.debug("🔍 Retrieving legacy message for order: {}", orderNumber);
            return null; // Placeholder
        } catch (Exception e) {
            log.error("❌ Error retrieving legacy message for order {}: {}", orderNumber, e.getMessage());
            return null;
        }
    }
} 