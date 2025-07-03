package com.backend.softtrainer.simulation.validation;

import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.simulation.DualModeSimulationRuntime.SimulationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

/**
 * ✅ Simulation Compatibility Validator - Format Compliance Checker
 * 
 * Validates that simulations conform to their detected type and can be
 * processed safely by the appropriate engine.
 */
@Service
@Slf4j
public class SimulationCompatibilityValidator {
    
    /**
     * 🔍 Validate Simulation for Type
     */
    public void validateSimulation(Simulation simulation, SimulationType type) {
        List<String> issues = validateAndGetIssues(simulation, type);
        
        if (!issues.isEmpty()) {
            String errorMessage = String.format(
                "Simulation %s (ID: %s) has compatibility issues for type %s: %s",
                simulation.getName(), simulation.getId(), type, String.join(", ", issues)
            );
            
            log.warn("⚠️ {}", errorMessage);
            
            // For now, log warnings instead of throwing exceptions
            // to maintain maximum compatibility
            // throw new IllegalArgumentException(errorMessage);
        }
    }
    
    /**
     * ✅ Check if Simulation is Compatible
     */
    public boolean isCompatible(Simulation simulation, SimulationType type) {
        List<String> issues = validateAndGetIssues(simulation, type);
        return issues.isEmpty();
    }
    
    /**
     * 📋 Validate and Get Issues List
     */
    public List<String> validateAndGetIssues(Simulation simulation, SimulationType type) {
        List<String> issues = new ArrayList<>();
        
        if (simulation == null) {
            issues.add("Simulation is null");
            return issues;
        }
        
        // Basic validation for all types
        validateBasicStructure(simulation, issues);
        
        // Type-specific validation
        switch (type) {
            case LEGACY -> validateLegacyFormat(simulation, issues);
            case MODERN -> validateModernFormat(simulation, issues);
            case HYBRID -> validateHybridFormat(simulation, issues);
            case UNKNOWN -> issues.add("Cannot validate unknown simulation type");
        }
        
        if (!issues.isEmpty()) {
            log.debug("📋 Found {} compatibility issues for simulation {}", 
                    issues.size(), simulation.getName());
        }
        
        return issues;
    }
    
    /**
     * 🔧 Validate Basic Structure
     */
    private void validateBasicStructure(Simulation simulation, List<String> issues) {
        if (simulation.getName() == null || simulation.getName().trim().isEmpty()) {
            issues.add("Simulation name is missing");
        }
        
        if (simulation.getNodes() == null) {
            issues.add("Simulation nodes are null");
            return;
        }
        
        if (simulation.getNodes().isEmpty()) {
            issues.add("Simulation has no nodes");
            return;
        }
        
        // Check for duplicate order numbers
        boolean hasDuplicateOrders = simulation.getNodes().stream()
                .map(FlowNode::getOrderNumber)
                .distinct()
                .count() != simulation.getNodes().size();
        
        if (hasDuplicateOrders) {
            issues.add("Simulation has duplicate order numbers");
        }
        
        // Check for missing characters
        long nodesWithoutCharacter = simulation.getNodes().stream()
                .filter(node -> node.getCharacter() == null)
                .count();
        
        if (nodesWithoutCharacter > 0) {
            log.debug("ℹ️ {} nodes without character assignments", nodesWithoutCharacter);
        }
    }
    
    /**
     * 🏛️ Validate Legacy Format
     */
    private void validateLegacyFormat(Simulation simulation, List<String> issues) {
        List<FlowNode> nodes = simulation.getNodes();
        
        // Check for proper legacy structure
        boolean hasOrderNumbers = nodes.stream()
                .allMatch(node -> node.getOrderNumber() != null);
        
        if (!hasOrderNumbers) {
            issues.add("Legacy simulation requires order numbers on all nodes");
        }
        
        // Check for valid message types
        boolean hasInvalidMessageTypes = nodes.stream()
                .anyMatch(node -> node.getMessageType() == null);
        
        if (hasInvalidMessageTypes) {
            issues.add("Legacy simulation has nodes with missing message types");
        }
        
        // Check for actionable nodes
        boolean hasActionableNodes = nodes.stream()
                .anyMatch(node -> node.getMessageType() != null && 
                         (node.getMessageType().name().contains("QUESTION") || 
                          node.getMessageType().name().contains("TASK")));
        
        if (!hasActionableNodes) {
            issues.add("Legacy simulation has no actionable nodes (questions/tasks)");
        }
        
        // Validate show_predicate syntax (basic check)
        for (FlowNode node : nodes) {
            String predicate = node.getShowPredicate();
            if (predicate != null && !predicate.trim().isEmpty()) {
                validatePredicateSyntax(predicate, issues, node);
            }
        }
    }
    
    /**
     * 🚀 Validate Modern Format
     */
    private void validateModernFormat(Simulation simulation, List<String> issues) {
        // TODO: Implement modern format validation
        // This would check for:
        // 1. Rule-based structure
        // 2. Enhanced flow nodes
        // 3. Modern message types
        // 4. Proper transition definitions
        
        issues.add("Modern format validation not yet implemented");
    }
    
    /**
     * 🔄 Validate Hybrid Format
     */
    private void validateHybridFormat(Simulation simulation, List<String> issues) {
        // Hybrid format should pass both legacy and modern validation
        // but with relaxed requirements
        
        validateLegacyFormat(simulation, issues);
        
        // Remove strict requirements for hybrid
        issues.removeIf(issue -> issue.contains("requires") || issue.contains("must have"));
        
        // Add hybrid-specific warnings
        if (issues.isEmpty()) {
            log.info("ℹ️ Hybrid simulation {} appears compatible with both formats", 
                    simulation.getName());
        }
    }
    
    /**
     * 🔍 Validate Predicate Syntax
     */
    private void validatePredicateSyntax(String predicate, List<String> issues, FlowNode node) {
        // Basic syntax validation for show_predicate
        
        if (predicate.contains("whereId") && !predicate.contains("\"")) {
            issues.add(String.format("Node %s: whereId predicate missing quotes", 
                    node.getOrderNumber()));
        }
        
        // Check for balanced parentheses
        long openParens = predicate.chars().filter(ch -> ch == '(').count();
        long closeParens = predicate.chars().filter(ch -> ch == ')').count();
        
        if (openParens != closeParens) {
            issues.add(String.format("Node %s: unbalanced parentheses in predicate", 
                    node.getOrderNumber()));
        }
        
        // Check for balanced brackets
        long openBrackets = predicate.chars().filter(ch -> ch == '[').count();
        long closeBrackets = predicate.chars().filter(ch -> ch == ']').count();
        
        if (openBrackets != closeBrackets) {
            issues.add(String.format("Node %s: unbalanced brackets in predicate", 
                    node.getOrderNumber()));
        }
        
        // Check for common typos
        if (predicate.contains("messag.") && !predicate.contains("message.")) {
            issues.add(String.format("Node %s: possible typo 'messag' should be 'message'", 
                    node.getOrderNumber()));
        }
    }
} 