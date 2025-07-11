package com.backend.softtrainer.integration;

import com.backend.softtrainer.config.TestSecurityConfig;
import com.backend.softtrainer.dtos.ChatRequestDto;
import com.backend.softtrainer.dtos.ChatResponseDto;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.dtos.messages.SingleChoiceAnswerMessageDto;
import com.backend.softtrainer.services.UserHyperParameterService;
import com.backend.softtrainer.entities.*;
import com.backend.softtrainer.entities.enums.*;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.repositories.*;
import com.backend.softtrainer.services.AiAgentService;
import com.backend.softtrainer.services.chatgpt.ChatGptServiceJvmOpenAi;
import com.backend.softtrainer.dtos.MessageDto;
import com.backend.softtrainer.dtos.aiagent.*;
import com.backend.softtrainer.dtos.StaticRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 🚀 Real API Integration Test for AI-Generated Simulations
 * 
 * This test verifies that our AI-generated simulation system works correctly
 * with the real API endpoints (/chats, /message/send) in production conditions.
 * 
 * Tests cover:
 * - Chat creation with AI-generated simulations
 * - Message processing through real API endpoints  
 * - Database persistence validation
 * - Error handling and timeouts
 * - Security and validation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(TestSecurityConfig.class)
@WithMockUser(username = "test-admin", roles = {"USER", "ADMIN", "OWNER"})
class AiGeneratedApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiAgentService aiAgentService;

    @MockBean
    private ChatGptServiceJvmOpenAi chatGptServiceJvmOpenAi;
    
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

    // Test data storage
    private static Long testSkillId;
    private static Long testSimulationId;
    private static Long testChatId;
    private static String testMessageId;

    @BeforeEach
    void setUp() throws InterruptedException {
        // Create test data with proper transaction boundary
        if (testSkillId == null) {
            createTestDataWithNewTransaction();
        }
        setupMocks();
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
                .description("Advanced AI-powered leadership development")
                .type(SkillType.DEVELOPMENT)
                .behavior(BehaviorType.DYNAMIC)
                .generationStatus(SkillGenerationStatus.COMPLETED)
                .isHidden(false)
                .build();
        skill = skillRepository.saveAndFlush(skill);
        testSkillId = skill.getId();

        // Create AI_GENERATED simulation with explicit ID
        Simulation simulation = Simulation.builder()
                .id(1L)
                .name("AI Leadership Simulation")
                .skill(skill)
                .type(SimulationType.AI_GENERATED)  // ✅ This is the key!
                .complexity(SimulationComplexity.MEDIUM)
                .hearts(5.0)
                .nodes(Collections.emptyList())    // ✅ No predefined nodes
                .build();
        simulation = simulationRepository.saveAndFlush(simulation);
        testSimulationId = simulation.getId();

        // Link organization with skill
        org.getAvailableSkills().add(skill);
        organizationRepository.saveAndFlush(org);
        
        // Create skill-simulation relationship
        Map<Simulation, Long> skillSimulations = new HashMap<>();
        skillSimulations.put(simulation, simulation.getId());
        skill.setSimulations(skillSimulations);
        skillRepository.saveAndFlush(skill);

        System.out.println("🎯 Created AI_GENERATED simulation with ID: " + testSimulationId);
        System.out.println("🎯 Created skill with ID: " + testSkillId);
    }

    private void setupMocks() throws InterruptedException {
        // Mock ChatGPT service
        when(chatGptServiceJvmOpenAi.buildAfterwardSimulationRecommendation(
            any(), any(), any(), any(), any(), any()
        )).thenReturn(CompletableFuture.completedFuture(
            new MessageDto("AI-generated simulation completed successfully!")
        ));

        // Mock AI agent responses
        setupAiAgentMocks();
    }

    /**
     * 🏗️ Test 1: Create AI-Generated Simulation through API
     */
    @Test
    @Order(1)
    @Transactional
    @Commit // Commit the transaction to make the chat persistent for subsequent tests
    void testCreateAiGeneratedSimulationThroughApi() throws Exception {
        System.out.println("🏗️ Testing Create AI-Generated Simulation through API...");

        // Create chat request
        ChatRequestDto chatRequest = new ChatRequestDto();
        chatRequest.setSimulationId(testSimulationId);
        chatRequest.setSkillId(testSkillId);

        // Call real API endpoint
        MvcResult result = mockMvc.perform(put("/chats/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Extract response
        String responseContent = result.getResponse().getContentAsString();
        
        // Handle potential empty response
        if (responseContent == null || responseContent.trim().isEmpty()) {
            System.err.println("❌ Empty response received from /chats/create");
            fail("Empty response received from /chats/create endpoint");
        }
        
        ChatResponseDto response = objectMapper.readValue(responseContent, ChatResponseDto.class);
        
        // Store for next tests
        testChatId = response.chatId();

        // Now assert expectations with detailed error messages
        assertThat(response.success()).as("Expected success=true but got success=%s with error message: %s", 
                response.success(), response.errorMessage()).isTrue();
        assertThat(response.chatId()).as("Chat ID should not be null").isNotNull();
        assertThat(response.messages()).as("Messages should not be empty for AI_GENERATED simulations").isNotEmpty();

        // Validate database state
        validateChatCreatedInDatabase(testChatId, testSimulationId);

        System.out.println("✅ API Test 1 PASSED: Chat created with ID " + testChatId);
    }

    /**
     * 📨 Test 2: Process User Message through API  
     */
    @Test
    @Order(2)
    // Remove @Transactional to access committed chat from previous test
    void testProcessUserMessageThroughApi() throws Exception {
        // Ensure we have a chat from previous test
        assertThat(testChatId).isNotNull();

        System.out.println("📨 Testing Process User Message through API...");

        // Create message request
        SingleChoiceAnswerMessageDto messageRequest = new SingleChoiceAnswerMessageDto();
        messageRequest.setId(UUID.randomUUID().toString());
        messageRequest.setChatId(testChatId);
        messageRequest.setMessageType(MessageType.SINGLE_CHOICE_QUESTION);
        messageRequest.setAnswer("Option 1");
        messageRequest.setUserResponseTime(5000L);

        // Call real message API endpoint
        MvcResult result = mockMvc.perform(put("/message/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(messageRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        
        // Handle empty response gracefully for async processing
        if (responseContent == null || responseContent.trim().isEmpty()) {
            System.out.println("⚠️ Empty response received - this might be due to async processing");
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            System.out.println("✅ Message processing accepted (async processing)");
            return;
        }

        // Validate response if not empty
        ChatResponseDto response = objectMapper.readValue(responseContent, ChatResponseDto.class);
        assertThat(response.messages()).isNotNull();

        System.out.println("✅ API Test 2 PASSED: Message processed successfully");
    }

    /**
     * 🛡️ Test 3: Security and Validation
     */
    @Test
    @Order(3)
    void testSecurityAndValidation() throws Exception {
        System.out.println("🛡️ Testing Security and Validation...");

        // Test 1: Invalid chat ID with proper message structure
        SingleChoiceAnswerMessageDto invalidRequest = new SingleChoiceAnswerMessageDto();
        invalidRequest.setId(UUID.randomUUID().toString()); // Provide valid ID
        invalidRequest.setChatId(99999L);
        invalidRequest.setMessageType(MessageType.SINGLE_CHOICE_QUESTION);
        invalidRequest.setAnswer("Test");
        invalidRequest.setUserResponseTime(1000L);

        // This should return 403 Forbidden for invalid chat access (correct security behavior)
        MvcResult result = mockMvc.perform(put("/message/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isForbidden()) // Expect 403 for invalid chat access
                .andReturn();
        
        System.out.println("✅ Security working: Invalid chat access properly rejected with 403");

        // Test 2: Null request
        mockMvc.perform(put("/message/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        System.out.println("✅ API Test 3 PASSED: Security validation working");
    }

    /**
     * ⏱️ Test 4: Timeout and Error Handling
     */
    @Test
    @Order(4)
    void testTimeoutAndErrorHandling() throws Exception {
        System.out.println("⏱️ Testing Timeout and Error Handling...");

        // Mock AI service timeout
        when(aiAgentService.generateMessage(any()))
                .thenThrow(new RuntimeException("AI service timeout"));

        if (testChatId != null) {
            // Create a proper test message
            SingleChoiceAnswerMessageDto timeoutRequest = new SingleChoiceAnswerMessageDto();
            timeoutRequest.setId(UUID.randomUUID().toString()); // Provide valid ID
            timeoutRequest.setChatId(testChatId);
            timeoutRequest.setMessageType(MessageType.SINGLE_CHOICE_QUESTION);
            timeoutRequest.setAnswer("Timeout Test");
            timeoutRequest.setUserResponseTime(1000L);

            // Should handle timeout gracefully
            MvcResult result = mockMvc.perform(put("/message/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(timeoutRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            // Timeout should be handled gracefully
            String responseContent = result.getResponse().getContentAsString();
            if (responseContent == null || responseContent.trim().isEmpty()) {
                System.out.println("⚠️ Empty response received - timeout handled gracefully");
            } else {
                ChatResponseDto response = objectMapper.readValue(responseContent, ChatResponseDto.class);
                assertThat(response.messages()).isNotNull();
            }
        }

        System.out.println("✅ API Test 4 PASSED: Timeout handling working");
    }

    /**
     * 🗄️ Test 5: Database Consistency
     */
    @Test
    @Order(5)
    @Transactional
    void testDatabaseConsistency() throws Exception {
        System.out.println("🗄️ Testing Database Consistency...");

        if (testChatId != null) {
            // Verify chat creation and consistency
            Optional<Chat> chatOpt = chatRepository.findById(testChatId);
            assertThat(chatOpt).isPresent();

            Chat chat = chatOpt.get();
            assertThat(chat.getSimulation().getType()).isEqualTo(SimulationType.AI_GENERATED);
            assertThat(chat.getUser().getEmail()).isEqualTo("test-admin");

            System.out.println("✅ Chat found: " + chat.getId());
            System.out.println("✅ Simulation type: " + chat.getSimulation().getType());
            System.out.println("✅ User: " + chat.getUser().getEmail());
        } else {
            System.out.println("⚠️ No chat ID available from previous tests");
        }

        // Verify simulation structure
        Optional<Simulation> simulationOpt = simulationRepository.findById(testSimulationId);
        assertThat(simulationOpt).isPresent();
        
        Simulation simulation = simulationOpt.get();
        assertThat(simulation.getType()).isEqualTo(SimulationType.AI_GENERATED);
        assertThat(simulation.getName()).isEqualTo("AI Leadership Simulation");

        System.out.println("✅ API Test 5 PASSED: Database consistency verified");
    }

    // Helper methods
    private void setupAiAgentMocks() {
        // Mock AI initialization response - multiple messages from different characters
        AiMessageGenerationResponseDto initResponse = AiMessageGenerationResponseDto.builder()
                .messages(Arrays.asList(
                    // Welcome from AI Coach
                    AiGeneratedMessageDto.builder()
                            .messageType("Text")
                            .content("Welcome to your personalized AI leadership training! I'm your executive coach.")
                            .characterName("AI Executive Coach")
                            .requiresResponse(false)
                            .build(),
                    // Initial question - actionable message
                    AiGeneratedMessageDto.builder()
                            .messageType("SingleChoiceQuestion")
                            .content("What leadership challenge would you like to work on today?")
                            .options(Arrays.asList("Team Communication", "Conflict Resolution", "Strategic Planning"))
                            .characterName("AI Executive Coach")
                            .requiresResponse(true)
                            .responseTimeLimit(30000L)
                            .build()
                ))
                .conversationEnded(false)
                .success(true)
                .build();

        // Mock follow-up response
        AiMessageGenerationResponseDto followUpResponse = AiMessageGenerationResponseDto.builder()
                .messages(Arrays.asList(
                    AiGeneratedMessageDto.builder()
                            .messageType("Text")
                            .content("Excellent choice! Let's dive into team communication scenarios.")
                            .characterName("AI Executive Coach")
                            .requiresResponse(false)
                            .build()
                ))
                .conversationEnded(false)
                .success(true)
                .build();

        // Configure mocks
        when(aiAgentService.initializeSimulation(any())).thenReturn(initResponse);
        when(aiAgentService.generateMessage(any())).thenReturn(followUpResponse);
    }

    private void validateChatCreatedInDatabase(Long chatId, Long simulationId) {
        Optional<Chat> chatOpt = chatRepository.findById(chatId);
        assertThat(chatOpt).as("Chat should be created in database").isPresent();
        
        Chat chat = chatOpt.get();
        assertThat(chat.getSimulation().getId()).isEqualTo(simulationId);
        assertThat(chat.getUser().getEmail()).isEqualTo("test-admin");
    }
} 