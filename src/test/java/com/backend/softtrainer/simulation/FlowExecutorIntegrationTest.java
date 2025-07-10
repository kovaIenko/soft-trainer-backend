package com.backend.softtrainer.simulation;

import com.backend.softtrainer.dtos.ChatDataDto;
import com.backend.softtrainer.entities.Character;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Organization;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.entities.enums.SimulationMode;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.messages.TextMessage;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.simulation.context.SimulationContextBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 🧪 Integration Tests for FlowExecutor
 *
 * Tests cover:
 * - All simulation modes (PREDEFINED, DYNAMIC, HYBRID)
 * - End-to-end flow processing
 * - Error handling and recovery
 * - Performance characteristics
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "logging.level.com.backend.softtrainer.simulation=DEBUG"
})
@DisplayName("FlowExecutor Integration Tests")
class FlowExecutorIntegrationTest {

    private FlowExecutor flowExecutor;
    private SimulationContextBuilder contextBuilder;

    private Chat testChat;
    private User testUser;
    private Simulation testSimulation;
    private Skill testSkill;
    private Character testCharacter;

    @BeforeEach
    void setUp() {
        // Create test entities
        Organization testOrg = Organization.builder()
            .id(1L)
            .name("Test Organization")
            .localization("en")
            .build();

        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .organization(testOrg)
            .build();

        testCharacter = Character.builder()
            .id(1L)
            .name("Test Character")
            .build();

        testSkill = Skill.builder()
            .id(1L)
            .name("Test Skill")
            .build();

        testSimulation = Simulation.builder()
            .id(1L)
            .name("Test Simulation")
            .hearts(5.0)
            .build();

        testChat = Chat.builder()
            .id(1L)
            .user(testUser)
            .simulation(testSimulation)
            .skill(testSkill)
            .hearts(5.0)
            .isFinished(false)
            .build();
    }

    @Nested
    @DisplayName("Simulation Mode Tests")
    class SimulationModeTests {

        @Test
        @DisplayName("Should handle PREDEFINED mode simulation")
        void shouldHandlePredefinedMode() {
            // Given
            SimulationContext context = createTestContext(SimulationMode.PREDEFINED);

            // When
            CompletableFuture<List<Message>> result = flowExecutor.initializeSimulation(context);

            // Then
            assertDoesNotThrow(() -> {
                List<Message> messages = result.get();
                assertNotNull(messages);
                // PREDEFINED mode should work with existing flow nodes
            });
        }

        @Test
        @DisplayName("Should handle DYNAMIC mode simulation")
        void shouldHandleDynamicMode() {
            // Given
            SimulationContext context = createTestContext(SimulationMode.DYNAMIC);

            // When
            CompletableFuture<List<Message>> result = flowExecutor.initializeSimulation(context);

            // Then
            assertDoesNotThrow(() -> {
                List<Message> messages = result.get();
                assertNotNull(messages);
                // DYNAMIC mode should generate AI content
            });
        }

        @Test
        @DisplayName("Should handle HYBRID mode simulation")
        void shouldHandleHybridMode() {
            // Given
            SimulationContext context = createTestContext(SimulationMode.HYBRID);

            // When
            CompletableFuture<List<Message>> result = flowExecutor.initializeSimulation(context);

            // Then
            assertDoesNotThrow(() -> {
                List<Message> messages = result.get();
                assertNotNull(messages);
                // HYBRID mode should combine predefined and AI content
            });
        }
    }

    @Nested
    @DisplayName("End-to-End Flow Tests")
    class EndToEndFlowTests {

        @Test
        @DisplayName("Should process complete simulation flow")
        void shouldProcessCompleteFlow() {
            // Given
            SimulationContext context = createTestContext(SimulationMode.PREDEFINED);

            // When - Initialize simulation
            CompletableFuture<List<Message>> initResult = flowExecutor.initializeSimulation(context);

            // Then
            assertDoesNotThrow(() -> {
                List<Message> initialMessages = initResult.get();
                assertNotNull(initialMessages);

                // Simulate user response
                Message userMessage = createTestUserMessage("Test response");
                context.addMessage(userMessage);

                // Process user input
                CompletableFuture<List<Message>> responseResult = flowExecutor.generateResponse(context, userMessage);
                List<Message> responseMessages = responseResult.get();

                assertNotNull(responseMessages);
            });
        }

        @Test
        @DisplayName("Should handle simulation completion")
        void shouldHandleSimulationCompletion() {
            // Given
            SimulationContext context = createTestContext(SimulationMode.PREDEFINED);
            context.updateHearts(0.0); // Trigger completion condition

            // When
            boolean isComplete = flowExecutor.isSimulationComplete(context);

            // Then
            assertTrue(isComplete);
        }

        @Test
        @DisplayName("Should process until actionable message")
        void shouldProcessUntilActionableMessage() {
            // Given
            SimulationContext context = createTestContext(SimulationMode.PREDEFINED);

            // When
            CompletableFuture<List<Message>> result = flowExecutor.initializeSimulation(context);

            // Then
            assertDoesNotThrow(() -> {
                List<Message> messages = result.get();

                // Should continue processing until an actionable message is found
                // or simulation is complete
                assertNotNull(messages);
            });
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle invalid context gracefully")
        void shouldHandleInvalidContext() {
            // Given
            SimulationContext invalidContext = SimulationContext.builder()
                .chatId(999L) // Non-existent chat
                .simulationMode(SimulationMode.PREDEFINED)
                .build();

            // When/Then
            assertDoesNotThrow(() -> {
                CompletableFuture<List<Message>> result = flowExecutor.initializeSimulation(invalidContext);
                List<Message> messages = result.get();

                // Should return fallback messages instead of throwing
                assertNotNull(messages);
            });
        }

        @Test
        @DisplayName("Should recover from rule evaluation errors")
        void shouldRecoverFromRuleErrors() {
            // Given
            SimulationContext context = createTestContext(SimulationMode.PREDEFINED);
            Message invalidMessage = createTestUserMessage(null); // Invalid message

            // When/Then
            assertDoesNotThrow(() -> {
                CompletableFuture<List<Message>> result = flowExecutor.generateResponse(context, invalidMessage);
                List<Message> messages = result.get();

                // Should handle error gracefully
                assertNotNull(messages);
            });
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should complete initialization within time limit")
        void shouldCompleteInitializationQuickly() {
            // Given
            SimulationContext context = createTestContext(SimulationMode.PREDEFINED);
            long startTime = System.currentTimeMillis();

            // When
            CompletableFuture<List<Message>> result = flowExecutor.initializeSimulation(context);

            // Then
            assertDoesNotThrow(() -> {
                List<Message> messages = result.get();
                long duration = System.currentTimeMillis() - startTime;

                assertNotNull(messages);
                assertTrue(duration < 5000, "Initialization should complete within 5 seconds");
            });
        }

        @Test
        @DisplayName("Should handle concurrent executions")
        void shouldHandleConcurrentExecutions() {
            // Given
            int concurrentExecutions = 5;
            List<CompletableFuture<List<Message>>> futures = new java.util.ArrayList<>();

            // When
            for (int i = 0; i < concurrentExecutions; i++) {
                SimulationContext context = createTestContext(SimulationMode.PREDEFINED);
                futures.add(flowExecutor.initializeSimulation(context));
            }

            // Then
            assertDoesNotThrow(() -> {
                CompletableFuture<Void> allOf = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
                );

                allOf.get(); // Wait for all to complete

                // Verify all completed successfully
                for (CompletableFuture<List<Message>> future : futures) {
                    List<Message> messages = future.get();
                    assertNotNull(messages);
                }
            });
        }
    }

    @Nested
    @DisplayName("Public API Tests")
    class PublicApiTests {

        @Test
        @DisplayName("Should support processUserInput API")
        void shouldSupportProcessUserInputApi() {
            // Given
            Message userMessage = createTestUserMessage("Test input");

            // When
            CompletableFuture<ChatDataDto> result = flowExecutor.processUserInput(userMessage, testChat);

            // Then
            assertDoesNotThrow(() -> {
                ChatDataDto chatData = result.get();

                assertNotNull(chatData);
                assertNotNull(chatData.messages());
                assertNotNull(chatData.params());
            });
        }

        @Test
        @DisplayName("Should support initializeSimulation API")
        void shouldSupportInitializeSimulationApi() {
            // When
            CompletableFuture<ChatDataDto> result = flowExecutor.initializeSimulation(testChat);

            // Then
            assertDoesNotThrow(() -> {
                ChatDataDto chatData = result.get();

                assertNotNull(chatData);
                assertNotNull(chatData.messages());
                assertNotNull(chatData.params());
                assertEquals(testChat.getHearts(), chatData.params().getHearts());
            });
        }
    }

    // Helper methods

    private SimulationContext createTestContext(SimulationMode mode) {
        return SimulationContext.builder()
            .chatId(testChat.getId())
            .chat(testChat)
            .user(testUser)
            .simulation(testSimulation)
            .skill(testSkill)
            .simulationMode(mode)
            .hearts(testChat.getHearts())
            .maxMessages(50)
            .build();
    }

    private Message createTestUserMessage(String content) {
        return TextMessage.builder()
            .id("test-message-" + System.currentTimeMillis())
            .content(content)
            .chat(testChat)
            .character(testCharacter)
            .messageType(MessageType.TEXT)
            .build();
    }
}
