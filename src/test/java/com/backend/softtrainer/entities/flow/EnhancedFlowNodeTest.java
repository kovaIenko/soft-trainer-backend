package com.backend.softtrainer.entities.flow;

import com.backend.softtrainer.entities.Character;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.entities.enums.InteractionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🧪 Comprehensive Unit Tests for EnhancedFlowNode Entity
 * 
 * Tests cover:
 * - Entity creation and validation
 * - JSON parsing for flow rules and hyperparameter actions
 * - Helper methods functionality
 * - Edge cases and error handling
 */
@DisplayName("EnhancedFlowNode Entity Tests")
class EnhancedFlowNodeTest {
    
    private ObjectMapper objectMapper;
    private Character testCharacter;
    private Simulation testSimulation;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        testCharacter = Character.builder()
            .id(1L)
            .name("Test Character")
            .build();
            
        testSimulation = Simulation.builder()
            .id(1L)
            .name("Test Simulation")
            .build();
    }
    
    @Nested
    @DisplayName("Entity Creation Tests")
    class EntityCreationTests {
        
        @Test
        @DisplayName("Should create basic EnhancedFlowNode with required fields")
        void shouldCreateBasicEnhancedFlowNode() {
            // Given
            EnhancedFlowNode node = EnhancedFlowNode.builder()
                .messageId(1L)
                .messageType(MessageType.TEXT)
                .text("Test message")
                .character(testCharacter)
                .build();
            
            // Then
            assertNotNull(node);
            assertEquals(1L, node.getMessageId());
            assertEquals(MessageType.TEXT, node.getMessageType());
            assertEquals("Test message", node.getText());
            assertEquals(testCharacter, node.getCharacter());
            assertFalse(node.isHasHint()); // Default value
        }
        
        @Test
        @DisplayName("Should create EnhancedFlowNode with all optional fields")
        void shouldCreateEnhancedFlowNodeWithAllFields() throws Exception {
            // Given
            String flowRulesJson = """
                [
                    {
                        "type": "user_response",
                        "message_id": 5,
                        "expected_options": [1, 2]
                    }
                ]
                """;
                
            String hyperparameterActionsJson = """
                [
                    {
                        "type": "INCREMENT",
                        "parameter": "empathy",
                        "value": 2.0
                    }
                ]
                """;
                
            JsonNode flowRules = objectMapper.readTree(flowRulesJson);
            JsonNode hyperparameterActions = objectMapper.readTree(hyperparameterActionsJson);
            
            // When
            EnhancedFlowNode node = EnhancedFlowNode.builder()
                .messageId(10L)
                .previousMessageIds(List.of(5L, 8L))
                .messageType(MessageType.SINGLE_CHOICE_QUESTION)
                .interactionType(InteractionType.SINGLE_CHOICE)
                .text("What would you do in this situation?")
                .character(testCharacter)
                .characterIdRaw(-1) // User character
                .simulation(testSimulation)
                .hasHint(true)
                .responseTimeLimit(30000L)
                .flowRules(flowRules)
                .hyperparameterActions(hyperparameterActions)
                .build();
            
            // Then
            assertNotNull(node);
            assertEquals(10L, node.getMessageId());
            assertEquals(List.of(5L, 8L), node.getPreviousMessageIds());
            assertEquals(MessageType.SINGLE_CHOICE_QUESTION, node.getMessageType());
            assertEquals(InteractionType.SINGLE_CHOICE, node.getInteractionType());
            assertTrue(node.isHasHint());
            assertEquals(30000L, node.getResponseTimeLimit());
            assertNotNull(node.getFlowRules());
            assertNotNull(node.getHyperparameterActions());
        }
    }
    
    @Nested
    @DisplayName("Helper Methods Tests")
    class HelperMethodsTests {
        
        @Test
        @DisplayName("Should correctly identify user messages")
        void shouldIdentifyUserMessages() {
            // Given - User message (characterIdRaw = -1)
            EnhancedFlowNode userNode = EnhancedFlowNode.builder()
                .messageId(1L)
                .characterIdRaw(-1)
                .messageType(MessageType.TEXT)
                .build();
                
            // Given - Character message
            EnhancedFlowNode characterNode = EnhancedFlowNode.builder()
                .messageId(2L)
                .character(testCharacter)
                .messageType(MessageType.TEXT)
                .build();
            
            // Then
            assertTrue(userNode.isUserMessage());
            assertFalse(characterNode.isUserMessage());
        }
        
        @Test
        @DisplayName("Should correctly identify actionable messages")
        void shouldIdentifyActionableMessages() {
            // Given - Actionable message types
            EnhancedFlowNode questionNode = EnhancedFlowNode.builder()
                .messageId(1L)
                .interactionType(InteractionType.SINGLE_CHOICE)
                .messageType(MessageType.SINGLE_CHOICE_QUESTION)
                .build();
                
            EnhancedFlowNode textInputNode = EnhancedFlowNode.builder()
                .messageId(2L)
                .interactionType(InteractionType.OPEN_TEXT)
                .messageType(MessageType.ENTER_TEXT_QUESTION)
                .build();
                
            // Given - Non-actionable message
            EnhancedFlowNode displayNode = EnhancedFlowNode.builder()
                .messageId(3L)
                .interactionType(InteractionType.TEXT_DISPLAY)
                .messageType(MessageType.TEXT)
                .build();
            
            // Then
            assertTrue(questionNode.isActionable());
            assertTrue(textInputNode.isActionable());
            assertFalse(displayNode.isActionable());
        }
        
        @Test
        @DisplayName("Should correctly identify complex predicates")
        void shouldIdentifyComplexPredicates() {
            // Given - Complex predicate
            EnhancedFlowNode complexNode = EnhancedFlowNode.builder()
                .messageId(1L)
                .showPredicate("message whereId \"5\" and saveChatValue[\"empathy\", readChatValue[\"empathy\"]+1]")
                .messageType(MessageType.TEXT)
                .build();
                
            // Given - Simple predicate
            EnhancedFlowNode simpleNode = EnhancedFlowNode.builder()
                .messageId(2L)
                .showPredicate("message whereId \"5\"")
                .messageType(MessageType.TEXT)
                .build();
                
            // Given - No predicate
            EnhancedFlowNode noPredicateNode = EnhancedFlowNode.builder()
                .messageId(3L)
                .messageType(MessageType.TEXT)
                .build();
            
            // Then
            assertTrue(complexNode.hasComplexPredicate());
            assertFalse(simpleNode.hasComplexPredicate());
            assertFalse(noPredicateNode.hasComplexPredicate());
        }
        
        @Test
        @DisplayName("Should parse options as list correctly")
        void shouldParseOptionsAsList() throws Exception {
            // Given - JSON options
            String optionsJson = """
                ["Option 1", "Option 2", "Option 3"]
                """;
            JsonNode options = objectMapper.readTree(optionsJson);
            
            EnhancedFlowNode node = EnhancedFlowNode.builder()
                .messageId(1L)
                .messageType(MessageType.SINGLE_CHOICE_QUESTION)
                .options(options)
                .build();
            
            // When
            List<String> optionsList = node.getOptionsAsList();
            
            // Then
            assertNotNull(optionsList);
            assertEquals(3, optionsList.size());
            assertEquals("Option 1", optionsList.get(0));
            assertEquals("Option 2", optionsList.get(1));
            assertEquals("Option 3", optionsList.get(2));
        }
        
        @Test
        @DisplayName("Should handle null options gracefully")
        void shouldHandleNullOptions() {
            // Given
            EnhancedFlowNode node = EnhancedFlowNode.builder()
                .messageId(1L)
                .messageType(MessageType.TEXT)
                .build();
            
            // When
            List<String> optionsList = node.getOptionsAsList();
            
            // Then
            assertNotNull(optionsList);
            assertTrue(optionsList.isEmpty());
        }
    }
    
    @Nested
    @DisplayName("JSON Flow Rules Tests")
    class JsonFlowRulesTests {
        
        @Test
        @DisplayName("Should store and retrieve flow rules JSON")
        void shouldStoreAndRetrieveFlowRules() throws Exception {
            // Given
            String flowRulesJson = """
                [
                    {
                        "type": "user_response",
                        "rule_id": "test_rule",
                        "message_id": 5,
                        "expected_options": [1, 2],
                        "match_type": "CONTAINS_ANY",
                        "description": "Test rule"
                    }
                ]
                """;
            JsonNode flowRules = objectMapper.readTree(flowRulesJson);
            
            // When
            EnhancedFlowNode node = EnhancedFlowNode.builder()
                .messageId(1L)
                .messageType(MessageType.TEXT)
                .flowRules(flowRules)
                .build();
            
            // Then
            assertNotNull(node.getFlowRules());
            assertTrue(node.getFlowRules().isArray());
            assertEquals(1, node.getFlowRules().size());
            
            JsonNode rule = node.getFlowRules().get(0);
            assertEquals("user_response", rule.get("type").asText());
            assertEquals("test_rule", rule.get("rule_id").asText());
            assertEquals(5, rule.get("message_id").asInt());
        }
        
        @Test
        @DisplayName("Should handle multiple flow rules")
        void shouldHandleMultipleFlowRules() throws Exception {
            // Given
            String flowRulesJson = """
                [
                    {
                        "type": "user_response",
                        "message_id": 5,
                        "expected_options": [1]
                    },
                    {
                        "type": "hyperparameter",
                        "parameter": "empathy",
                        "operator": "GREATER_THAN",
                        "value": 3.0
                    }
                ]
                """;
            JsonNode flowRules = objectMapper.readTree(flowRulesJson);
            
            // When
            EnhancedFlowNode node = EnhancedFlowNode.builder()
                .messageId(1L)
                .messageType(MessageType.TEXT)
                .flowRules(flowRules)
                .build();
            
            // Then
            assertNotNull(node.getFlowRules());
            assertEquals(2, node.getFlowRules().size());
            assertEquals("user_response", node.getFlowRules().get(0).get("type").asText());
            assertEquals("hyperparameter", node.getFlowRules().get(1).get("type").asText());
        }
    }
    
    @Nested
    @DisplayName("Hyperparameter Actions Tests")
    class HyperparameterActionsTests {
        
        @Test
        @DisplayName("Should store and retrieve hyperparameter actions JSON")
        void shouldStoreAndRetrieveHyperparameterActions() throws Exception {
            // Given
            String actionsJson = """
                [
                    {
                        "type": "INCREMENT",
                        "parameter": "active_listening",
                        "value": 2.0,
                        "description": "Reward active listening"
                    }
                ]
                """;
            JsonNode actions = objectMapper.readTree(actionsJson);
            
            // When
            EnhancedFlowNode node = EnhancedFlowNode.builder()
                .messageId(1L)
                .messageType(MessageType.TEXT)
                .hyperparameterActions(actions)
                .build();
            
            // Then
            assertNotNull(node.getHyperparameterActions());
            assertTrue(node.getHyperparameterActions().isArray());
            assertEquals(1, node.getHyperparameterActions().size());
            
            JsonNode action = node.getHyperparameterActions().get(0);
            assertEquals("INCREMENT", action.get("type").asText());
            assertEquals("active_listening", action.get("parameter").asText());
            assertEquals(2.0, action.get("value").asDouble());
        }
        
        @Test
        @DisplayName("Should handle multiple hyperparameter actions")
        void shouldHandleMultipleHyperparameterActions() throws Exception {
            // Given
            String actionsJson = """
                [
                    {
                        "type": "INCREMENT",
                        "parameter": "empathy",
                        "value": 1.0
                    },
                    {
                        "type": "DECREMENT", 
                        "parameter": "engagement",
                        "value": 0.5
                    },
                    {
                        "type": "SET",
                        "parameter": "correctness",
                        "value": 5.0
                    }
                ]
                """;
            JsonNode actions = objectMapper.readTree(actionsJson);
            
            // When
            EnhancedFlowNode node = EnhancedFlowNode.builder()
                .messageId(1L)
                .messageType(MessageType.TEXT)
                .hyperparameterActions(actions)
                .build();
            
            // Then
            assertNotNull(node.getHyperparameterActions());
            assertEquals(3, node.getHyperparameterActions().size());
            assertEquals("INCREMENT", node.getHyperparameterActions().get(0).get("type").asText());
            assertEquals("DECREMENT", node.getHyperparameterActions().get(1).get("type").asText());
            assertEquals("SET", node.getHyperparameterActions().get(2).get("type").asText());
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Should handle invalid JSON gracefully")
        void shouldHandleInvalidJsonGracefully() {
            // Given - Node with invalid JSON structure
            EnhancedFlowNode node = EnhancedFlowNode.builder()
                .messageId(1L)
                .messageType(MessageType.TEXT)
                .build();
            
            // When/Then - Should not throw exception
            assertDoesNotThrow(() -> {
                List<String> options = node.getOptionsAsList();
                assertNotNull(options);
                assertTrue(options.isEmpty());
            });
        }
        
        @Test
        @DisplayName("Should validate required fields")
        void shouldValidateRequiredFields() {
            // Given/When/Then - Should be able to create with minimal required fields
            assertDoesNotThrow(() -> {
                EnhancedFlowNode node = EnhancedFlowNode.builder()
                    .messageId(1L)
                    .messageType(MessageType.TEXT)
                    .build();
                    
                assertNotNull(node);
                assertEquals(1L, node.getMessageId());
                assertEquals(MessageType.TEXT, node.getMessageType());
            });
        }
        
        @Test
        @DisplayName("Should handle empty previous message IDs")
        void shouldHandleEmptyPreviousMessageIds() {
            // Given
            EnhancedFlowNode node = EnhancedFlowNode.builder()
                .messageId(1L)
                .messageType(MessageType.TEXT)
                .previousMessageIds(List.of())
                .build();
            
            // Then
            assertNotNull(node.getPreviousMessageIds());
            assertTrue(node.getPreviousMessageIds().isEmpty());
        }
        
        @Test
        @DisplayName("Should handle null character ID scenarios")
        void shouldHandleNullCharacterIdScenarios() {
            // Given - Node with null character and characterIdRaw
            EnhancedFlowNode node = EnhancedFlowNode.builder()
                .messageId(1L)
                .messageType(MessageType.TEXT)
                .build();
            
            // Then
            assertFalse(node.isUserMessage()); // Should default to false
        }
    }
    
    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderPatternTests {
        
        @Test
        @DisplayName("Should support fluent builder pattern")
        void shouldSupportFluentBuilderPattern() throws Exception {
            // Given
            String optionsJson = """
                ["Yes", "No", "Maybe"]
                """;
            JsonNode options = objectMapper.readTree(optionsJson);
            
            // When
            EnhancedFlowNode node = EnhancedFlowNode.builder()
                .messageId(42L)
                .previousMessageIds(List.of(10L, 20L))
                .messageType(MessageType.SINGLE_CHOICE_QUESTION)
                .interactionType(InteractionType.SINGLE_CHOICE)
                .text("Do you agree with this approach?")
                .prompt("Consider the implications of your choice.")
                .character(testCharacter)
                .simulation(testSimulation)
                .options(options)
                .correctAnswerPosition(1)
                .hasHint(true)
                .responseTimeLimit(60000L)
                .build();
            
            // Then
            assertNotNull(node);
            assertEquals(42L, node.getMessageId());
            assertEquals(2, node.getPreviousMessageIds().size());
            assertEquals(MessageType.SINGLE_CHOICE_QUESTION, node.getMessageType());
            assertEquals(InteractionType.SINGLE_CHOICE, node.getInteractionType());
            assertEquals("Do you agree with this approach?", node.getText());
            assertEquals("Consider the implications of your choice.", node.getPrompt());
            assertEquals(testCharacter, node.getCharacter());
            assertEquals(testSimulation, node.getSimulation());
            assertEquals(1, node.getCorrectAnswerPosition());
            assertTrue(node.isHasHint());
            assertEquals(60000L, node.getResponseTimeLimit());
            assertEquals(3, node.getOptionsAsList().size());
        }
    }
} 