package com.backend.softtrainer.integration;

import com.backend.softtrainer.config.TestSecurityConfig;
import com.backend.softtrainer.dtos.ChatRequestDto;
import com.backend.softtrainer.dtos.ChatResponseDto;
import com.backend.softtrainer.dtos.StaticRole;
import com.backend.softtrainer.dtos.messages.EnterTextAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.SingleChoiceAnswerMessageDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Organization;
import com.backend.softtrainer.entities.Role;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.entities.enums.BehaviorType;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.entities.enums.SimulationType;
import com.backend.softtrainer.entities.enums.SkillGenerationStatus;
import com.backend.softtrainer.entities.enums.SkillType;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.messages.TextMessage;
import com.backend.softtrainer.repositories.CharacterRepository;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.repositories.MessageRepository;
import com.backend.softtrainer.repositories.OrganizationRepository;
import com.backend.softtrainer.repositories.RoleRepository;
import com.backend.softtrainer.repositories.SimulationRepository;
import com.backend.softtrainer.repositories.SkillRepository;
import com.backend.softtrainer.repositories.UserRepository;
import com.backend.softtrainer.services.chatgpt.ChatGptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 🚀 Real AI Agent Integration Test for AI-Generated Simulations
 *
 * This test verifies that our AI-generated simulation system works correctly
 * with the REAL AI agent at http://16.171.20.54:8000
 *
 * ⚠️ CRITICAL: This test MUST FAIL if AI agent returns 404, timeouts, or fallback logic is triggered
 *
 * Tests cover:
 * - Real AI agent communication validation (no 404/timeouts allowed)
 * - Chat creation with AI-generated simulations (real AI responses only)
 * - Message processing through real API endpoints
 * - Database persistence validation
 * - Real AI response content validation (no fallback placeholders)
 * - End-to-end conversation flow with proper async handling
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(TestSecurityConfig.class)
@WithMockUser(username = "test-admin", roles = {"USER", "ADMIN", "OWNER"})
@ExtendWith(OutputCaptureExtension.class)
@Transactional
public class AiGeneratedRealAgentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Repository dependencies for validation
    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SimulationRepository simulationRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @MockBean
    private ChatGptService chatGptService;

    // Test data storage
    private static Long testSkillId;
    private static Long testSimulationId;
    private static Long testChatId;
    private static String testMessageId;

    // Fallback indicators to detect when real AI agent fails
    private static final String[] FALLBACK_INDICATORS = {
        "Creating fallback initial messages",
        "AI agent initialization failed",
        "AI Agent is unavailable",
        "status=404 NOT_FOUND",
        "Creating default welcome message",
        "Using fallback content",
        "Fallback logic triggered"
    };

    @BeforeEach
    void setUp() throws InterruptedException {
        // Create test data with proper transaction boundary
        if (testSkillId == null) {
            createTestDataWithNewTransaction();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTestDataWithNewTransaction() {
        createTestUserIfNotExists();
        createTestSimulation();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTestUserIfNotExists() {
        if (userRepository.findByEmail("test-admin").isEmpty()) {
            // Create roles if not exist
            Role userRole = roleRepository.findByName(StaticRole.ROLE_USER).orElseGet(() -> {
                Role role = new Role();
                role.setId(1L);
                role.setName(StaticRole.ROLE_USER);
                return roleRepository.saveAndFlush(role);
            });

            Role adminRole = roleRepository.findByName(StaticRole.ROLE_ADMIN).orElseGet(() -> {
                Role role = new Role();
                role.setId(2L);
                role.setName(StaticRole.ROLE_ADMIN);
                return roleRepository.saveAndFlush(role);
            });

            Role ownerRole = roleRepository.findByName(StaticRole.ROLE_OWNER).orElseGet(() -> {
                Role role = new Role();
                role.setId(3L);
                role.setName(StaticRole.ROLE_OWNER);
                return roleRepository.saveAndFlush(role);
            });

            // Create organization first
            Organization org = organizationRepository.findById(1L).orElseGet(() -> {
                Organization newOrg = new Organization();
                newOrg.setId(1L);
                newOrg.setName("SoftTrainer");
                newOrg.setAvailableSkills(new HashSet<>());
                return organizationRepository.saveAndFlush(newOrg);
            });

            // Create test user with roles
            User testUser = User.builder()
                    .id(1L)
                    .email("test-admin")
                    .username("test-admin")
                    .password("$2a$10$E6lEIWn7DyKGqPNIQHnAkuUFwGYTk1q2fGGvnEQcZlFEqoGi5HGpG") // 'password'
                    .organization(org)
                    .roles(Set.of(userRole, adminRole, ownerRole))
                    .build();

            userRepository.saveAndFlush(testUser);
            System.out.println("✅ Created test user: test-admin");
        }
    }

    private void createTestSimulation() {
        // Create organization with explicit ID
        Organization org = organizationRepository.findById(1L).orElseGet(() -> {
            Organization newOrg = Organization.builder()
                    .id(1L)
                    .name("SoftTrainer")
                    .availableSkills(new HashSet<>())
                    .build();
            return organizationRepository.saveAndFlush(newOrg);
        });

        // Create skill for AI-generated simulations with explicit ID
        Skill skill = Skill.builder()
                .id(1L)
                .name("AI Leadership Training")
                .description("Advanced AI-powered leadership development with real-time conversation")
                .type(SkillType.DEVELOPMENT)
                .behavior(BehaviorType.DYNAMIC)
                .generationStatus(SkillGenerationStatus.COMPLETED)
                .isHidden(false)
                .simulationCount(3)
                .build();
        skill = skillRepository.saveAndFlush(skill);
        testSkillId = skill.getId();

        // --- FIX: Add skill to organization's availableSkills and save (avoid duplicates) ---
        if (!org.getAvailableSkills().contains(skill)) {
            org.getAvailableSkills().add(skill);
            organizationRepository.saveAndFlush(org);
        }
        // --- END FIX ---

        // Create AI_GENERATED simulation with explicit ID
        Simulation simulation = Simulation.builder()
                .id(1L)
                .name("AI Leadership Simulation")
                .type(SimulationType.AI_GENERATED)
                .skill(skill)
                .hearts(3.0)
                .build();
        simulation = simulationRepository.saveAndFlush(simulation);
        testSimulationId = simulation.getId();

        // --- FIX: Add simulation to skill's simulations map and save skill ---
        skill.getSimulations().put(simulation, 1L);
        skillRepository.saveAndFlush(skill);
        // --- END FIX ---

        System.out.println("✅ Created AI-generated simulation: " + simulation.getId());
    }

    /**
     * Validates that no fallback logic was triggered during the test
     */
    private void validateNoFallbackTriggered(CapturedOutput output) {
        String logs = output.getAll();

        for (String indicator : FALLBACK_INDICATORS) {
            if (logs.contains(indicator)) {
                fail(String.format("❌ CRITICAL FAILURE: Fallback logic detected in logs: '%s'. " +
                        "This indicates AI agent failed (404, timeout, or unavailable). " +
                        "Full logs: %s", indicator, logs));
            }
        }

        // Additional validation for HTTP error codes
        if (logs.contains("status=404") || logs.contains("NOT_FOUND")) {
            fail("❌ CRITICAL FAILURE: AI Agent returned 404 NOT_FOUND. " +
                    "This test requires real AI agent responses, not fallback logic.");
        }

        if (logs.contains("timeout") || logs.contains("TimeoutException")) {
            fail("❌ CRITICAL FAILURE: AI Agent request timed out. " +
                    "This indicates connectivity issues with the real AI agent.");
        }
    }

    /**
     * Validates that response contains real AI-generated content, not fallback placeholders
     */
    private void validateRealAiContent(ChatResponseDto response) {
        assertThat(response.success())
                .withFailMessage("❌ Chat creation failed: %s", response.errorMessage())
                .isTrue();

        assertThat(response.chatId())
                .withFailMessage("❌ Chat ID should not be null")
                .isNotNull();

        assertThat(response.messages())
                .withFailMessage("❌ Messages should not be empty for AI_GENERATED simulations")
                .isNotEmpty();

        // Basic validation - ensure we have actual messages with proper structure
        response.messages().forEach(message -> {
            assertThat(message.getMessageType())
                    .withFailMessage("❌ Message type should not be null")
                    .isNotNull();

            assertThat(message.getId())
                    .withFailMessage("❌ Message ID should not be null")
                    .isNotNull();

            // Note: UserMessageDto doesn't expose content directly in the API response
            // Content validation will be done at the persistence level in validatePersistedMessagesAreRealAi
        });
    }

    /**
     * 🏗️ Test 1: Create AI-Generated Simulation with Real AI Agent - STRICT VALIDATION
     */
    @Test
    @Order(1)
    @Transactional
    @Commit
    void testCreateAiGeneratedSimulationWithRealAgent(CapturedOutput output) throws Exception {
        System.out.println("🏗️ Testing Create AI-Generated Simulation with Real AI Agent...");
        System.out.println("🚨 STRICT MODE: Test will FAIL if AI agent returns 404, timeouts, or fallback logic is triggered");

        // Create chat request
        ChatRequestDto chatRequest = new ChatRequestDto();
        chatRequest.setSimulationId(testSimulationId);
        chatRequest.setSkillId(testSkillId);

        // Call real API endpoint with timeout protection
        MvcResult result = mockMvc.perform(put("/chats/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Extract response
        String responseContent = result.getResponse().getContentAsString();

        // STRICT: Empty response = FAILURE
        if (responseContent == null || responseContent.trim().isEmpty()) {
            fail("❌ CRITICAL FAILURE: Empty response received from /chats/create. " +
                    "This indicates AI agent communication failure or async processing issues.");
        }

        // Wait for async processing to complete (if any)
        Thread.sleep(3000);

        // Validate no fallback logic was triggered
        validateNoFallbackTriggered(output);

        ChatResponseDto response = objectMapper.readValue(responseContent, ChatResponseDto.class);

        // Store for next tests
        testChatId = response.chatId();

        // Validate real AI content (no fallback)
        validateRealAiContent(response);

        // Validate database state with proper wait for async operations
        waitForChatPersistence(testChatId);
        validateChatCreatedInDatabase(testChatId, testSimulationId);

        System.out.println("✅ Real AI Agent Test 1 PASSED: Chat created with ID " + testChatId);
        System.out.println("✅ Validated real AI-generated content with " + response.messages().size() + " messages");
        System.out.println("✅ No fallback logic was triggered - AI agent responded successfully");
    }

    /**
     * Waits for chat persistence to complete (handles async operations)
     */
    private void waitForChatPersistence(Long chatId) throws InterruptedException {
        int maxAttempts = 10;
        int attempt = 0;

        while (attempt < maxAttempts) {
            Optional<Chat> chatOpt = chatRepository.findById(chatId);
            if (chatOpt.isPresent()) {
                // Wait a bit more for messages to be persisted
                Thread.sleep(1000);
                return;
            }

            Thread.sleep(500);
            attempt++;
        }

        fail(String.format("❌ CRITICAL FAILURE: Chat with ID %d was not persisted after %d attempts. " +
                "This indicates async persistence issues.", chatId, maxAttempts));
    }

    /**
     * 📨 Test 2: Process User Message with Real AI Agent - STRICT VALIDATION
     */
    @Test
    @Order(2)
    void testProcessUserMessageWithRealAgent(CapturedOutput output) throws Exception {
        // Ensure we have a chat from previous test
        assertThat(testChatId).isNotNull();

        System.out.println("📨 Testing Process User Message with Real AI Agent...");
        System.out.println("🚨 STRICT MODE: Test will FAIL if AI agent returns 404, timeouts, or empty responses");

        // Create message request
        SingleChoiceAnswerMessageDto messageRequest = new SingleChoiceAnswerMessageDto();
        messageRequest.setId(UUID.randomUUID().toString());
        messageRequest.setChatId(testChatId);
        messageRequest.setMessageType(MessageType.SINGLE_CHOICE_QUESTION);
        messageRequest.setAnswer("Team Communication");
        messageRequest.setUserResponseTime(5000L);

        // Call real message API endpoint
        MvcResult result = mockMvc.perform(put("/message/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(messageRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();

        // Wait for async processing
        Thread.sleep(5000);

        // Validate no fallback logic was triggered
        validateNoFallbackTriggered(output);

        // STRICT: For AI-generated simulations, we expect meaningful responses, not empty ones
        if (responseContent == null || responseContent.trim().isEmpty()) {
            // Check if message was persisted despite empty response (async processing)
            List<Message> messages = messageRepository.findAll().stream()
                    .filter(m -> m.getChat() != null && m.getChat().getId().equals(testChatId))
                    .toList();

            if (messages.isEmpty()) {
                fail("❌ CRITICAL FAILURE: Empty response AND no messages persisted. " +
                        "This indicates complete AI agent communication failure.");
            } else {
                System.out.println("⚠️ Empty response but messages found in DB - async processing detected");
                // Validate the persisted messages are real AI content
                validatePersistedMessagesAreRealAi(messages);
            }
        } else {
            // Validate response if not empty
            ChatResponseDto response = objectMapper.readValue(responseContent, ChatResponseDto.class);
            validateRealAiContent(response);
            System.out.println("✅ Received " + response.messages().size() + " messages from real AI agent");
        }

        System.out.println("✅ Real AI Agent Test 2 PASSED: Message processed successfully");
    }

    /**
     * Validates that persisted messages contain real AI content, not fallback
     */
    private void validatePersistedMessagesAreRealAi(List<Message> messages) {
        for (Message message : messages) {
            if (message instanceof TextMessage textMessage) {
                String content = textMessage.getContent();
                if (content != null) {
                    String lowerContent = content.toLowerCase();

                    if (lowerContent.contains("fallback") ||
                        lowerContent.contains("default") ||
                        lowerContent.contains("placeholder") ||
                        lowerContent.contains("unavailable")) {
                        fail(String.format("❌ CRITICAL FAILURE: Persisted message contains fallback content: '%s'", content));
                    }
                }
            }
        }
    }

    /**
     * 💬 Test 3: Multi-turn Conversation with Real AI Agent - STRICT VALIDATION
     */
    @Test
    @Order(3)
    void testMultiTurnConversationWithRealAgent(CapturedOutput output) throws Exception {
        assertThat(testChatId).isNotNull();

        System.out.println("💬 Testing Multi-turn Conversation with Real AI Agent...");
        System.out.println("🚨 STRICT MODE: Each turn must receive real AI responses");

        // First message - text response
        EnterTextAnswerMessageDto textRequest = new EnterTextAnswerMessageDto();
        textRequest.setId(UUID.randomUUID().toString());
        textRequest.setChatId(testChatId);
        textRequest.setMessageType(MessageType.ENTER_TEXT_QUESTION);
        textRequest.setAnswer("I want to improve my team communication skills and learn active listening techniques");
        textRequest.setUserResponseTime(3000L);

        MvcResult result1 = mockMvc.perform(put("/message/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(textRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Wait for AI processing
        Thread.sleep(5000);

        // Validate no fallback after first message
        validateNoFallbackTriggered(output);

        String responseContent1 = result1.getResponse().getContentAsString();
        if (responseContent1 != null && !responseContent1.trim().isEmpty()) {
            ChatResponseDto response1 = objectMapper.readValue(responseContent1, ChatResponseDto.class);
            validateRealAiContent(response1);
            System.out.println("✅ Turn 1: Received " + response1.messages().size() + " real AI messages");
        }

        // Second message - choice response
        SingleChoiceAnswerMessageDto choiceRequest = new SingleChoiceAnswerMessageDto();
        choiceRequest.setId(UUID.randomUUID().toString());
        choiceRequest.setChatId(testChatId);
        choiceRequest.setMessageType(MessageType.SINGLE_CHOICE_QUESTION);
        choiceRequest.setAnswer("Active Listening");
        choiceRequest.setUserResponseTime(4000L);

        MvcResult result2 = mockMvc.perform(put("/message/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(choiceRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Wait for AI processing
        Thread.sleep(5000);

        // Validate no fallback after second message
        validateNoFallbackTriggered(output);

        String responseContent2 = result2.getResponse().getContentAsString();
        if (responseContent2 != null && !responseContent2.trim().isEmpty()) {
            ChatResponseDto response2 = objectMapper.readValue(responseContent2, ChatResponseDto.class);
            validateRealAiContent(response2);
            System.out.println("✅ Turn 2: Received " + response2.messages().size() + " real AI messages");
        }

        System.out.println("✅ Real AI Agent Test 3 PASSED: Multi-turn conversation with real AI responses only");
    }

    /**
     * 🗄️ Test 4: Database Persistence Validation - ENHANCED
     */
    @Test
    @Order(4)
    @Transactional
    void testDatabasePersistenceWithRealAgent() throws Exception {
        System.out.println("🗄️ Testing Database Persistence with Real AI Agent...");

        if (testChatId != null) {
            // Verify chat creation and consistency
            Optional<Chat> chatOpt = chatRepository.findById(testChatId);
            assertThat(chatOpt).isPresent();

            Chat chat = chatOpt.get();
            assertThat(chat.getSimulation().getType()).isEqualTo(SimulationType.AI_GENERATED);
            assertThat(chat.getUser().getEmail()).isEqualTo("test-admin");

            // Verify messages were persisted AND contain real AI content
            List<Message> messages = messageRepository.findAll().stream()
                    .filter(m -> m.getChat() != null && m.getChat().getId().equals(testChatId))
                    .toList();
            assertThat(messages).isNotEmpty();

            // Validate persisted messages are real AI content
            validatePersistedMessagesAreRealAi(messages);

            System.out.println("✅ Chat found: " + chat.getId());
            System.out.println("✅ Simulation type: " + chat.getSimulation().getType());
            System.out.println("✅ User: " + chat.getUser().getEmail());
            System.out.println("✅ Real AI messages persisted: " + messages.size());
        } else {
            System.out.println("⚠️ No chat ID available from previous tests");
        }

        // Verify simulation structure
        Optional<Simulation> simulationOpt = simulationRepository.findById(testSimulationId);
        assertThat(simulationOpt).isPresent();

        Simulation simulation = simulationOpt.get();
        assertThat(simulation.getType()).isEqualTo(SimulationType.AI_GENERATED);
        assertThat(simulation.getName()).isEqualTo("AI Leadership Simulation");

        System.out.println("✅ Real AI Agent Test 4 PASSED: Database persistence with real AI content verified");
    }

    /**
     * 🔍 Test 5: AI Agent Response Validation - COMPREHENSIVE
     */
    @Test
    @Order(5)
    void testAiAgentResponseValidation() throws Exception {
        System.out.println("🔍 Testing AI Agent Response Validation...");

        if (testChatId != null) {
            // Get messages from database
            List<Message> messages = messageRepository.findAll().stream()
                    .filter(m -> m.getChat() != null && m.getChat().getId().equals(testChatId))
                    .toList();
            assertThat(messages).isNotEmpty();

            // Validate message structure AND content quality
            for (Message message : messages) {
                assertThat(message.getMessageType()).isNotNull();
                assertThat(message.getChat().getId()).isEqualTo(testChatId);

                // Check if it's a TextMessage to access content
                if (message instanceof TextMessage textMessage) {
                    assertThat(textMessage.getContent()).isNotNull();
                    assertThat(textMessage.getContent()).isNotEmpty();

                    // Validate content quality (real AI should provide substantial content)
                    String content = textMessage.getContent();
                    assertThat(content.length())
                            .withFailMessage("Real AI content should be substantial, got: %s", content)
                            .isGreaterThan(10);

                    // Validate no fallback indicators in content
                    String lowerContent = content.toLowerCase();
                    for (String indicator : FALLBACK_INDICATORS) {
                        assertThat(lowerContent)
                                .withFailMessage("Message contains fallback indicator '%s': %s", indicator, content)
                                .doesNotContain(indicator.toLowerCase());
                    }

                    System.out.println("✅ Real AI message validated: " + message.getMessageType() + " - " +
                        content.substring(0, Math.min(50, content.length())) + "...");
                } else {
                    System.out.println("✅ Message validated: " + message.getMessageType() + " (non-text message)");
                }
            }
        }

        System.out.println("✅ Real AI Agent Test 5 PASSED: Comprehensive AI response validation successful");
    }

    // Helper methods
    private void validateChatCreatedInDatabase(Long chatId, Long simulationId) {
        Optional<Chat> chatOpt = chatRepository.findById(chatId);
        assertThat(chatOpt).as("Chat should be created in database").isPresent();

        Chat chat = chatOpt.get();
        assertThat(chat.getSimulation().getId()).isEqualTo(simulationId);
        assertThat(chat.getUser().getEmail()).isEqualTo("test-admin");

        System.out.println("✅ Database validation passed: Chat " + chatId + " properly linked to simulation " + simulationId);
    }
}
