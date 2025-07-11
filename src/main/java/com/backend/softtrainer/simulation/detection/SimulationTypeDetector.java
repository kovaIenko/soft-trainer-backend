package com.backend.softtrainer.simulation.detection;

import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.simulation.DualModeSimulationRuntime.SimulationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 🔍 Simulation Type Detector - Automatic Format Recognition
 * 
 * Analyzes simulation structure to determine processing approach:
 * - LEGACY: Contains show_predicate fields, uses old message format
 * - MODERN: Uses structured rules, modern flow definitions
 * - HYBRID: Mixed format (handles gracefully)
 * - UNKNOWN: Invalid or unrecognized format
 */
@Service
@Slf4j
public class SimulationTypeDetector {
    
    // Patterns for detecting legacy predicate complexity
    private static final Pattern COMPLEX_PREDICATE_PATTERN = Pattern.compile(
            ".*(saveChatValue|readChatValue|whereId|message\\.|anyCorrect|selected).*"
    );
    
    private static final Pattern SIMPLE_PREDICATE_PATTERN = Pattern.compile(
            "^\\s*(true|false|1|0)\\s*$"
    );
    
    /**
     * 🎯 Main Detection Method
     */
    public SimulationType detectSimulationType(Simulation simulation) {
        if (simulation == null) {
            log.warn("⚠️ Null simulation provided for type detection");
            return SimulationType.UNKNOWN;
        }
        
        // Check explicit simulation type field first (for AI-generated simulations)
        if (simulation.getType() == com.backend.softtrainer.entities.enums.SimulationType.AI_GENERATED) {
            log.info("🤖 Detected AI-generated simulation: {}", simulation.getName());
            return SimulationType.AI_GENERATED;
        }
        
        try {
            if (simulation.getNodes() == null || simulation.getNodes().isEmpty()) {
                log.warn("⚠️ Empty or null nodes in simulation for type detection");
                return SimulationType.UNKNOWN;
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // If we can't access nodes due to lazy loading, assume it's a legacy simulation
            // This ensures backward compatibility
            log.debug("🔍 Cannot access nodes due to lazy loading, assuming LEGACY simulation");
            return SimulationType.LEGACY;
        }
        
        try {
            log.debug("🔍 Analyzing simulation: {} with {} nodes", 
                    simulation.getName(), simulation.getNodes().size());
            
            DetectionMetrics metrics = analyzeSimulationStructure(simulation);
            
            SimulationType detectedType = determineTypeFromMetrics(metrics);
            
            log.info("📊 Simulation {} detected as: {} (confidence: {})", 
                    simulation.getName(), detectedType, metrics.getConfidenceScore());
            
            return detectedType;
        } catch (org.hibernate.LazyInitializationException e) {
            // If we encounter lazy loading issues during analysis, default to LEGACY
            log.debug("🔍 LazyInitializationException during analysis, defaulting to LEGACY simulation");
            return SimulationType.LEGACY;
        }
    }
    
    /**
     * 📊 Analyze Simulation Structure
     */
    private DetectionMetrics analyzeSimulationStructure(Simulation simulation) {
        DetectionMetrics metrics = new DetectionMetrics();
        
        List<FlowNode> nodes = simulation.getNodes();
        
        for (FlowNode node : nodes) {
            analyzeNode(node, metrics);
        }
        
        // Calculate confidence and final metrics
        metrics.calculateConfidence();
        
        log.debug("📈 Analysis complete: {} legacy indicators, {} modern indicators", 
                metrics.legacyIndicatorCount, metrics.modernIndicatorCount);
        
        return metrics;
    }
    
    /**
     * 🔍 Analyze Individual Node
     */
    private void analyzeNode(FlowNode node, DetectionMetrics metrics) {
        metrics.totalNodes++;
        
        // Check for legacy show_predicate
        String predicate = node.getShowPredicate();
        if (predicate != null && !predicate.trim().isEmpty()) {
            metrics.nodesWithPredicates++;
            
            if (COMPLEX_PREDICATE_PATTERN.matcher(predicate).matches()) {
                metrics.complexPredicateCount++;
                metrics.legacyIndicatorCount += 3; // Strong legacy indicator
            } else if (SIMPLE_PREDICATE_PATTERN.matcher(predicate).matches()) {
                metrics.simplePredicateCount++;
                metrics.legacyIndicatorCount += 1; // Weak legacy indicator
            } else {
                metrics.customPredicateCount++;
                metrics.legacyIndicatorCount += 2; // Medium legacy indicator
            }
        }
        
        // Check for modern rule structures
        if (hasModernRuleStructure(node)) {
            metrics.modernIndicatorCount += 3; // Strong modern indicator
            log.debug("🚀 Found modern rule structure in node: {}", node.getId());
        }
        
        // Check message type patterns
        analyzeMessageTypePatterns(node, metrics);
        
        // Check node relationships and dependencies
        analyzeNodeRelationships(node, metrics);
    }
    
    /**
     * 🔮 Check for Modern Rule Structure
     */
    private boolean hasModernRuleStructure(FlowNode node) {
        // Check if node has flow_rules defined (modern format)
        return node.getFlowRules() != null && !node.getFlowRules().isEmpty();
    }
    
    /**
     * 📨 Analyze Message Type Patterns
     */
    private void analyzeMessageTypePatterns(FlowNode node, DetectionMetrics metrics) {
        if (node.getMessageType() != null) {
            switch (node.getMessageType()) {
                case TEXT, SINGLE_CHOICE_QUESTION, MULTI_CHOICE_TASK, ENTER_TEXT_QUESTION, HINT_MESSAGE, RESULT_SIMULATION -> {
                    metrics.legacyMessageTypeCount++;
                    metrics.legacyIndicatorCount += 1;
                }
                default -> {
                    // Unknown message types might indicate modern format
                    metrics.unknownMessageTypeCount++;
                }
            }
        }
    }
    
    /**
     * 🔗 Analyze Node Relationships
     */
    private void analyzeNodeRelationships(FlowNode node, DetectionMetrics metrics) {
        // Check ordering pattern (legacy uses orderNumber/previousOrderNumber)
        if (node.getOrderNumber() != null && node.getPreviousOrderNumber() != null) {
            metrics.legacyOrderingCount++;
            metrics.legacyIndicatorCount += 1;
        }
    }
    
    /**
     * 🎯 Determine Type from Metrics
     */
    private SimulationType determineTypeFromMetrics(DetectionMetrics metrics) {
        // Strong legacy indicators
        if (metrics.legacyIndicatorCount > 0 && metrics.modernIndicatorCount == 0) {
            return SimulationType.LEGACY;
        }
        
        // Strong modern indicators
        if (metrics.modernIndicatorCount > 0 && metrics.legacyIndicatorCount == 0) {
            return SimulationType.MODERN;
        }
        
        // Mixed indicators
        if (metrics.legacyIndicatorCount > 0 && metrics.modernIndicatorCount > 0) {
            return SimulationType.HYBRID;
        }
        
        // No clear indicators but has nodes
        if (metrics.totalNodes > 0) {
            // Default to legacy for maximum compatibility
            log.info("🔧 No clear type indicators found, defaulting to LEGACY for safety");
            return SimulationType.LEGACY;
        }
        
        return SimulationType.UNKNOWN;
    }
    
    /**
     * 📊 Detection Metrics Container
     */
    private static class DetectionMetrics {
        int totalNodes = 0;
        int nodesWithPredicates = 0;
        int complexPredicateCount = 0;
        int simplePredicateCount = 0;
        int customPredicateCount = 0;
        int legacyMessageTypeCount = 0;
        int unknownMessageTypeCount = 0;
        int legacyOrderingCount = 0;
        
        int legacyIndicatorCount = 0;
        int modernIndicatorCount = 0;
        
        double confidenceScore = 0.0;
        
        void calculateConfidence() {
            if (totalNodes == 0) {
                confidenceScore = 0.0;
                return;
            }
            
            int totalIndicators = legacyIndicatorCount + modernIndicatorCount;
            if (totalIndicators == 0) {
                confidenceScore = 0.3; // Low confidence for unclear formats
                return;
            }
            
            // Confidence based on indicator clarity and consistency
            double indicatorRatio = Math.max(legacyIndicatorCount, modernIndicatorCount) / (double) totalIndicators;
            double nodecoverage = nodesWithPredicates / (double) totalNodes;
            
            confidenceScore = Math.min(1.0, indicatorRatio * 0.7 + nodecoverage * 0.3);
        }
        
        double getConfidenceScore() {
            return confidenceScore;
        }
    }
} 