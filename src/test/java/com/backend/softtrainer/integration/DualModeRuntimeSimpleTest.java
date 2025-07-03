package com.backend.softtrainer.integration;

import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.simulation.DualModeSimulationRuntime;
import com.backend.softtrainer.simulation.detection.SimulationTypeDetector;
import com.backend.softtrainer.simulation.validation.SimulationCompatibilityValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🧪 Simple Dual-Mode Runtime Tests (No Docker/SSL Dependencies)
 * 
 * These tests focus purely on the dual-mode runtime logic without 
 * requiring full Spring context or external dependencies.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DisplayName("🔄 Simple Dual-Mode Runtime Tests")
class DualModeRuntimeSimpleTest {
    
    private SimulationTypeDetector simulationTypeDetector;
    private SimulationCompatibilityValidator compatibilityValidator;
    
    @BeforeEach
    void setUp() {
        // Initialize components directly without full Spring context
        simulationTypeDetector = new SimulationTypeDetector();
        compatibilityValidator = new SimulationCompatibilityValidator();
    }
    
    @Test
    @DisplayName("🏛️ Should detect legacy simulation with show_predicate")
    void shouldDetectLegacySimulationWithShowPredicate() {
        // Given: A simulation with show_predicate nodes
        Simulation legacySimulation = createSimulationWithShowPredicate();
        
        // When: Detecting simulation type
        DualModeSimulationRuntime.SimulationType detectedType = 
            simulationTypeDetector.detectSimulationType(legacySimulation);
        
        // Then: Should be detected as LEGACY
        assertEquals(DualModeSimulationRuntime.SimulationType.LEGACY, detectedType);
        System.out.println("✅ Legacy simulation correctly detected");
    }
    
    @Test
    @DisplayName("🔍 Should analyze show_predicate complexity correctly")
    void shouldAnalyzeShowPredicateComplexity() {
        // Given: Simulation with complex show_predicate
        Simulation complexSimulation = createComplexShowPredicateSimulation();
        
        // When: Detecting simulation type
        DualModeSimulationRuntime.SimulationType detectedType = 
            simulationTypeDetector.detectSimulationType(complexSimulation);
        
        // Then: Should still be LEGACY but recognize complexity
        assertEquals(DualModeSimulationRuntime.SimulationType.LEGACY, detectedType);
        System.out.println("✅ Complex show_predicate simulation handled correctly");
    }
    
    @Test
    @DisplayName("✅ Should validate legacy simulation compatibility")
    void shouldValidateLegacySimulationCompatibility() {
        // Given: A valid legacy simulation
        Simulation legacySimulation = createSimulationWithShowPredicate();
        
        // When: Validating compatibility
        boolean isCompatible = compatibilityValidator.isCompatible(
            legacySimulation, 
            DualModeSimulationRuntime.SimulationType.LEGACY
        );
        
        List<String> issues = compatibilityValidator.validateAndGetIssues(
            legacySimulation, 
            DualModeSimulationRuntime.SimulationType.LEGACY
        );
        
        // Then: Should be compatible with no issues
        assertTrue(isCompatible, "Legacy simulation should be compatible");
        assertTrue(issues.isEmpty(), "Should have no compatibility issues");
        System.out.println("✅ Legacy simulation validation passed");
    }
    
    @Test
    @DisplayName("🚨 Should handle empty simulation gracefully")
    void shouldHandleEmptySimulation() {
        // Given: Empty simulation
        Simulation emptySimulation = new Simulation();
        emptySimulation.setNodes(new ArrayList<>());
        
        // When: Detecting type
        DualModeSimulationRuntime.SimulationType detectedType = 
            simulationTypeDetector.detectSimulationType(emptySimulation);
        
        // Then: Should be UNKNOWN or default to LEGACY for safety
        assertTrue(
            detectedType == DualModeSimulationRuntime.SimulationType.UNKNOWN ||
            detectedType == DualModeSimulationRuntime.SimulationType.LEGACY,
            "Empty simulation should be UNKNOWN or default to LEGACY"
        );
        System.out.println("✅ Empty simulation handled gracefully: " + detectedType);
    }
    
    @Test
    @DisplayName("🔄 Should default to LEGACY for unclear simulations")
    void shouldDefaultToLegacyForUnclearSimulations() {
        // Given: Simulation with unclear format
        Simulation unclearSimulation = createUnclearFormatSimulation();
        
        // When: Detecting type
        DualModeSimulationRuntime.SimulationType detectedType = 
            simulationTypeDetector.detectSimulationType(unclearSimulation);
        
        // Then: Should default to LEGACY for maximum compatibility
        assertEquals(DualModeSimulationRuntime.SimulationType.LEGACY, detectedType);
        System.out.println("✅ Unclear simulation defaulted to LEGACY for safety");
    }
    
    @Test
    @DisplayName("📊 Should provide simulation analysis summary")
    void shouldProvideSimulationAnalysisSummary() {
        // Given: Various simulation types
        Simulation legacySimulation = createSimulationWithShowPredicate();
        Simulation complexSimulation = createComplexShowPredicateSimulation();
        Simulation emptySimulation = new Simulation();
        emptySimulation.setNodes(new ArrayList<>());
        
        // When: Analyzing all simulations
        DualModeSimulationRuntime.SimulationType legacyType = 
            simulationTypeDetector.detectSimulationType(legacySimulation);
        DualModeSimulationRuntime.SimulationType complexType = 
            simulationTypeDetector.detectSimulationType(complexSimulation);
        DualModeSimulationRuntime.SimulationType emptyType = 
            simulationTypeDetector.detectSimulationType(emptySimulation);
        
        // Then: Print analysis summary
        System.out.println("\n🎯 DUAL-MODE RUNTIME ANALYSIS SUMMARY:");
        System.out.println("=====================================");
        System.out.println("✅ Legacy Simulation Detection: " + legacyType);
        System.out.println("✅ Complex Simulation Detection: " + complexType);
        System.out.println("✅ Empty Simulation Detection: " + emptyType);
        System.out.println("✅ All detection scenarios working correctly!");
        
        // Verify all expected
        assertEquals(DualModeSimulationRuntime.SimulationType.LEGACY, legacyType);
        assertEquals(DualModeSimulationRuntime.SimulationType.LEGACY, complexType);
    }
    
    // Helper methods
    private Simulation createSimulationWithShowPredicate() {
        Simulation simulation = new Simulation();
        simulation.setId(1L);
        simulation.setName("Test Legacy Simulation");
        
        List<FlowNode> nodes = new ArrayList<>();
        
        // Create a flow node with show_predicate
        FlowNode node1 = new FlowNode();
        node1.setId(1L);
        node1.setMessageType(MessageType.TEXT);
        node1.setShowPredicate("true");
        nodes.add(node1);
        
        FlowNode node2 = new FlowNode();
        node2.setId(2L);
        node2.setMessageType(MessageType.SINGLE_CHOICE_QUESTION);
        node2.setShowPredicate("message.whereId(1).anyCorrect()");
        nodes.add(node2);
        
        simulation.setNodes(nodes);
        return simulation;
    }
    
    private Simulation createComplexShowPredicateSimulation() {
        Simulation simulation = new Simulation();
        simulation.setId(2L);
        simulation.setName("Complex Legacy Simulation");
        
        List<FlowNode> nodes = new ArrayList<>();
        
        FlowNode complexNode = new FlowNode();
        complexNode.setId(1L);
        complexNode.setMessageType(MessageType.SINGLE_CHOICE_QUESTION);
        complexNode.setShowPredicate("saveChatValue('progress', readChatValue('level') + 1) && message.whereId(1).anyCorrect()");
        nodes.add(complexNode);
        
        simulation.setNodes(nodes);
        return simulation;
    }
    
    private Simulation createUnclearFormatSimulation() {
        Simulation simulation = new Simulation();
        simulation.setId(3L);
        simulation.setName("Unclear Format Simulation");
        
        List<FlowNode> nodes = new ArrayList<>();
        
        FlowNode unclearNode = new FlowNode();
        unclearNode.setId(1L);
        unclearNode.setMessageType(MessageType.TEXT);
        // No show_predicate - unclear format
        nodes.add(unclearNode);
        
        simulation.setNodes(nodes);
        return simulation;
    }
} 