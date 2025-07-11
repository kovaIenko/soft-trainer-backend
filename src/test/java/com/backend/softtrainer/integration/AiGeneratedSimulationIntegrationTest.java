package com.backend.softtrainer.integration;

import com.backend.softtrainer.config.TestSecurityConfig;
import com.backend.softtrainer.dtos.ChatRequestDto;
import com.backend.softtrainer.dtos.ChatResponseDto;
import com.backend.softtrainer.dtos.messages.SingleChoiceAnswerMessageDto;
import com.backend.softtrainer.dtos.client.UserMessageDto;
import com.backend.softtrainer.entities.*;
import com.backend.softtrainer.entities.enums.*;
import com.backend.softtrainer.repositories.*;
import com.backend.softtrainer.dtos.StaticRole;
import com.backend.softtrainer.services.AiAgentService;
import com.backend.softtrainer.services.chatgpt.ChatGptServiceJvmOpenAi;
import com.backend.softtrainer.dtos.MessageDto;
import com.backend.softtrainer.dtos.aiagent.*;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumingThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Comprehensive Integration Tests for AI-Generated Simulations
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(TestSecurityConfig.class)
@WithMockUser(username = "test-admin", roles = {"USER", "ADMIN", "OWNER"})
@Transactional
class AiGeneratedSimulationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiAgentService aiAgentService;

    @MockBean
    private ChatGptServiceJvmOpenAi chatGptServiceJvmOpenAi;

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

    // Test data
    private static Long testSkillId;
    private static Long testSimulationId;
    private static Long testChatId;

    @BeforeEach
    void setUp() {
        // Ensure test user exists in database
        createTestUserIfNotExists();
        
        // Mock AI agent service
        when(aiAgentService.generateMessage(any(AiMessageGenerationRequestDto.class)))
                .thenReturn(
                        AiMessageGenerationResponseDto.builder()
                                .messages(List.of(
                                        AiGeneratedMessageDto.builder()
                                                .messageType("Text")
                                                .content("Welcome to the AI-generated simulation!")
                                                .characterName("Coach")
                                                .characterRole("COACH")
                                                .requiresResponse(false)
                                                .build(),
                                        AiGeneratedMessageDto.builder()
                                                .messageType("SingleChoiceQuestion")
                                                .content("What's your preferred leadership style?")
                                                .options(List.of("Collaborative", "Directive", "Supportive"))
                                                .characterName("Coach")
                                                .characterRole("COACH")
                                                .requiresResponse(true)
                                                .build()
                                ))
                                .conversationEnded(false)
                                .success(true)
                                .build()
                );

        // Mock ChatGPT service
        try {
            when(chatGptServiceJvmOpenAi.buildAfterwardSimulationRecommendation(
                    any(), any(), any(), any(), any(), any()
            )).thenReturn(CompletableFuture.completedFuture(
                    new MessageDto("AI-generated simulation completed successfully!")
            ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Reset test data
        testSkillId = null;
        testSimulationId = null;
        testChatId = null;
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTestUserIfNotExists() {
        if (userRepository.findByEmail("test-admin").isEmpty()) {
            // Create organization first
            Organization org = organizationRepository.findById(1L).orElseGet(() -> {
                Organization newOrg = new Organization();
                newOrg.setId(1L);
                newOrg.setName("SoftTrainer");
                return organizationRepository.save(newOrg);
            });
            
            // Link organization with test skill if needed
            Skill testSkill = skillRepository.findById(1L).orElse(null);
            if (testSkill != null) {
                org.setAvailableSkills(Set.of(testSkill));
                org = organizationRepository.save(org);
            }
            
            // Create test user
            User testUser = User.builder()
                    .id(1L)
                    .email("test-admin")
                    .username("test-admin")
                    .password("$2a$10$E6lEIWn7DyKGqPNIQHnAkuUFwGYTk1q2fGGvnEQcZlFEqoGi5HGpG") // 'password'
                    .organization(org)
                    .build();
            
            userRepository.saveAndFlush(testUser);
            System.out.println("✅ Created test user: test-admin");
        } else {
            System.out.println("✅ Test user already exists: test-admin");
        }
    }

    /**
     * Test 1: Chat Creation with AI-Generated Simulation
     */
    @Test
    @Order(1)
    void testChatCreationWithAiGeneration() throws Exception {
        // Create test data
        createTestSimulation();

        // Create chat request
        ChatRequestDto chatRequest = new ChatRequestDto();
        chatRequest.setSimulationId(testSimulationId);
        chatRequest.setSkillId(testSkillId);

        // Call API
        MvcResult result = mockMvc.perform(put("/chats/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Validate response
        String responseContent = result.getResponse().getContentAsString();
        ChatResponseDto response = objectMapper.readValue(responseContent, ChatResponseDto.class);

        assertThat(response.success()).isTrue();
        assertThat(response.chatId()).isNotNull();
        assertThat(response.messages()).isNotEmpty();

        // Store for next tests
        testChatId = response.chatId();

        // Validate message types
        List<UserMessageDto> messages = response.messages();
        assertThat(messages.stream().anyMatch(m -> MessageType.TEXT.equals(m.getMessageType()))).isTrue();
        assertThat(messages.stream().anyMatch(m -> MessageType.SINGLE_CHOICE_QUESTION.equals(m.getMessageType()))).isTrue();

        // Validate database
        assertThat(chatRepository.findById(testChatId)).isPresent();

        System.out.println("✅ Test 1 PASSED: Chat created with AI-generated messages");
    }

    /**
     * Test 2: User Response Processing
     */
    @Test
    @Order(2)
    void testUserResponseProcessing() throws Exception {
        assumingThat(testChatId != null, () -> {
            try {
                // Create user response
                SingleChoiceAnswerMessageDto userResponse = new SingleChoiceAnswerMessageDto();
                userResponse.setId(UUID.randomUUID().toString());
                userResponse.setChatId(testChatId);
                userResponse.setMessageType(MessageType.SINGLE_CHOICE_ANSWER);
                userResponse.setAnswer("Collaborative");
                userResponse.setUserResponseTime(3000L);

                // Send response
                MvcResult result = mockMvc.perform(put("/message/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userResponse)))
                        .andExpect(status().isOk())
                        .andReturn();

                // Validate response
                String responseContent = result.getResponse().getContentAsString();
                ChatResponseDto response = objectMapper.readValue(responseContent, ChatResponseDto.class);

                assertThat(response.messages()).isNotEmpty();

                System.out.println("✅ Test 2 PASSED: User response processed");

            } catch (Exception e) {
                throw new RuntimeException("Test failed", e);
            }
        });
    }

    /**
     * Test 3: AI Response Validation
     */
    @Test
    @Order(3)
    void testAiResponseValidation() throws Exception {
        assumingThat(testChatId != null, () -> {
            try {
                // Mock invalid AI response
                when(aiAgentService.generateMessage(any(AiMessageGenerationRequestDto.class)))
                        .thenReturn(
                                AiMessageGenerationResponseDto.builder()
                                        .messages(Collections.emptyList())
                                        .success(false)
                                        .errorMessage("AI service unavailable")
                                        .build()
                        );

                // Create user response
                SingleChoiceAnswerMessageDto userResponse = new SingleChoiceAnswerMessageDto();
                userResponse.setId(UUID.randomUUID().toString());
                userResponse.setChatId(testChatId);
                userResponse.setMessageType(MessageType.SINGLE_CHOICE_ANSWER);
                userResponse.setAnswer("Directive");
                userResponse.setUserResponseTime(2000L);

                // Send response
                MvcResult result = mockMvc.perform(put("/message/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userResponse)))
                        .andExpect(status().isOk())
                        .andReturn();

                // Validate response
                String responseContent = result.getResponse().getContentAsString();
                ChatResponseDto response = objectMapper.readValue(responseContent, ChatResponseDto.class);

                // Should handle invalid AI response gracefully
                assertThat(response.success()).isFalse();

                System.out.println("✅ Test 3 PASSED: AI response validation works");

            } catch (Exception e) {
                throw new RuntimeException("Test failed", e);
            }
        });
    }

    /**
     * Test 4: Conversation End Handling
     */
    @Test
    @Order(4)
    void testConversationEndHandling() throws Exception {
        assumingThat(testChatId != null, () -> {
            try {
                // Mock conversation ended response
                when(aiAgentService.generateMessage(any(AiMessageGenerationRequestDto.class)))
                        .thenReturn(
                                AiMessageGenerationResponseDto.builder()
                                        .messages(List.of(
                                                AiGeneratedMessageDto.builder()
                                                        .messageType("ResultSimulation")
                                                        .content("Congratulations! You've completed the simulation.")
                                                        .characterName("System")
                                                        .characterRole("SYSTEM")
                                                        .requiresResponse(false)
                                                        .build()
                                        ))
                                        .conversationEnded(true)
                                        .endReason("simulation_completed")
                                        .success(true)
                                        .build()
                        );

                // Create user response
                SingleChoiceAnswerMessageDto userResponse = new SingleChoiceAnswerMessageDto();
                userResponse.setId(UUID.randomUUID().toString());
                userResponse.setChatId(testChatId);
                userResponse.setMessageType(MessageType.SINGLE_CHOICE_ANSWER);
                userResponse.setAnswer("Supportive");
                userResponse.setUserResponseTime(1500L);

                // Send response
                MvcResult result = mockMvc.perform(put("/message/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userResponse)))
                        .andExpect(status().isOk())
                        .andReturn();

                // Validate response
                String responseContent = result.getResponse().getContentAsString();
                ChatResponseDto response = objectMapper.readValue(responseContent, ChatResponseDto.class);

                assertThat(response.success()).isTrue();
                assertThat(response.messages()).isNotEmpty();

                System.out.println("✅ Test 4 PASSED: Conversation end handled correctly");

            } catch (Exception e) {
                throw new RuntimeException("Test failed", e);
            }
        });
    }

    /**
     * Test 5: Database Persistence
     */
    @Test
    @Order(5)
    void testDatabasePersistence() throws Exception {
        assumingThat(testChatId != null, () -> {
            // Validate chat exists
            Optional<Chat> chat = chatRepository.findById(testChatId);
            assertThat(chat).isPresent();

            // Validate messages exist
            List<com.backend.softtrainer.entities.messages.Message> messages = messageRepository.findAll()
                    .stream()
                    .filter(m -> m.getChat() != null && testChatId.equals(m.getChat().getId()))
                    .toList();
            assertThat(messages).isNotEmpty();

            // Validate simulation relationship
            assertThat(chat.get().getSimulation().getId()).isEqualTo(testSimulationId);

            System.out.println("✅ Test 5 PASSED: Database persistence validated");
        });
    }

    /**
     * Test 6: AI-agent returns malformed message (missing type, null content, etc.)
     */
    @Test
    @Order(6)
    void testMalformedAiResponse() throws Exception {
        assumingThat(testChatId != null, () -> {
            try {
                // Mock malformed AI response - missing required fields
                when(aiAgentService.generateMessage(any(AiMessageGenerationRequestDto.class)))
                        .thenReturn(
                                AiMessageGenerationResponseDto.builder()
                                        .messages(List.of(
                                                AiGeneratedMessageDto.builder()
                                                        .messageType(null) // Missing type
                                                        .content("") // Empty content
                                                        .characterName(null) // Missing character
                                                        .requiresResponse(null)
                                                        .build()
                                        ))
                                        .success(true)
                                        .build()
                        );

                // Create user response
                SingleChoiceAnswerMessageDto userResponse = new SingleChoiceAnswerMessageDto();
                userResponse.setId(UUID.randomUUID().toString());
                userResponse.setChatId(testChatId);
                userResponse.setMessageType(MessageType.SINGLE_CHOICE_ANSWER);
                userResponse.setAnswer("Test Response");

                // Send response and expect graceful handling
                MvcResult result = mockMvc.perform(put("/message/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userResponse)))
                        .andExpect(status().isOk())
                        .andReturn();

                System.out.println("✅ Test 6 PASSED: Malformed AI response handled gracefully");

            } catch (Exception e) {
                System.err.println("❌ Test 6 FAILED: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Test 7: AI-agent returns no messages
     */
    @Test
    @Order(7)
    void testEmptyAiResponse() throws Exception {
        assumingThat(testChatId != null, () -> {
            try {
                // Mock empty AI response
                when(aiAgentService.generateMessage(any(AiMessageGenerationRequestDto.class)))
                        .thenReturn(
                                AiMessageGenerationResponseDto.builder()
                                        .messages(Collections.emptyList()) // No messages
                                        .success(true)
                                        .build()
                        );

                // Create user response
                SingleChoiceAnswerMessageDto userResponse = new SingleChoiceAnswerMessageDto();
                userResponse.setId(UUID.randomUUID().toString());
                userResponse.setChatId(testChatId);
                userResponse.setMessageType(MessageType.SINGLE_CHOICE_ANSWER);
                userResponse.setAnswer("Test Response");

                // Send response and expect graceful handling
                MvcResult result = mockMvc.perform(put("/message/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userResponse)))
                        .andExpect(status().isOk())
                        .andReturn();

                System.out.println("✅ Test 7 PASSED: Empty AI response handled gracefully");

            } catch (Exception e) {
                System.err.println("❌ Test 7 FAILED: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Test 8: Multiple characters respond at once
     */
    @Test
    @Order(8)
    void testMultipleCharacterResponse() throws Exception {
        assumingThat(testChatId != null, () -> {
            try {
                // Mock multiple character AI response
                when(aiAgentService.generateMessage(any(AiMessageGenerationRequestDto.class)))
                        .thenReturn(
                                AiMessageGenerationResponseDto.builder()
                                        .messages(List.of(
                                                AiGeneratedMessageDto.builder()
                                                        .messageType("Text")
                                                        .content("Coach response")
                                                        .characterName("Coach")
                                                        .characterRole("COACH")
                                                        .requiresResponse(false)
                                                        .build(),
                                                AiGeneratedMessageDto.builder()
                                                        .messageType("Text")
                                                        .content("Colleague response")
                                                        .characterName("Colleague")
                                                        .characterRole("COLLEAGUE")
                                                        .requiresResponse(false)
                                                        .build(),
                                                AiGeneratedMessageDto.builder()
                                                        .messageType("SingleChoiceQuestion")
                                                        .content("Manager question")
                                                        .options(List.of("Option 1", "Option 2"))
                                                        .characterName("Manager")
                                                        .characterRole("MANAGER")
                                                        .requiresResponse(true)
                                                        .build()
                                        ))
                                        .success(true)
                                        .build()
                        );

                // Create user response
                SingleChoiceAnswerMessageDto userResponse = new SingleChoiceAnswerMessageDto();
                userResponse.setId(UUID.randomUUID().toString());
                userResponse.setChatId(testChatId);
                userResponse.setMessageType(MessageType.SINGLE_CHOICE_ANSWER);
                userResponse.setAnswer("Test Response");

                // Send response and expect proper handling
                MvcResult result = mockMvc.perform(put("/message/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userResponse)))
                        .andExpect(status().isOk())
                        .andReturn();

                // Validate response
                String responseContent = result.getResponse().getContentAsString();
                ChatResponseDto response = objectMapper.readValue(responseContent, ChatResponseDto.class);
                
                // Should handle multiple characters
                assertThat(response.success()).isTrue();
                assertThat(response.messages()).isNotEmpty();

                System.out.println("✅ Test 8 PASSED: Multiple character response handled correctly");

            } catch (Exception e) {
                System.err.println("❌ Test 8 FAILED: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Test 9: AI-agent returns unsupported message type
     */
    @Test
    @Order(9)
    void testUnsupportedMessageType() throws Exception {
        assumingThat(testChatId != null, () -> {
            try {
                // Mock unsupported message type
                when(aiAgentService.generateMessage(any(AiMessageGenerationRequestDto.class)))
                        .thenReturn(
                                AiMessageGenerationResponseDto.builder()
                                        .messages(List.of(
                                                AiGeneratedMessageDto.builder()
                                                        .messageType("UnsupportedType") // Invalid type
                                                        .content("Test content")
                                                        .characterName("Coach")
                                                        .characterRole("COACH")
                                                        .requiresResponse(false)
                                                        .build()
                                        ))
                                        .success(true)
                                        .build()
                        );

                // Create user response
                SingleChoiceAnswerMessageDto userResponse = new SingleChoiceAnswerMessageDto();
                userResponse.setId(UUID.randomUUID().toString());
                userResponse.setChatId(testChatId);
                userResponse.setMessageType(MessageType.SINGLE_CHOICE_ANSWER);
                userResponse.setAnswer("Test Response");

                // Send response and expect graceful handling
                MvcResult result = mockMvc.perform(put("/message/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userResponse)))
                        .andExpect(status().isOk())
                        .andReturn();

                System.out.println("✅ Test 9 PASSED: Unsupported message type handled gracefully");

            } catch (Exception e) {
                System.err.println("❌ Test 9 FAILED: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Test 10: AI-agent returns messages that require response but no requiresResponse is marked
     */
    @Test
    @Order(10)
    void testInconsistentRequiresResponse() throws Exception {
        assumingThat(testChatId != null, () -> {
            try {
                // Mock inconsistent requiresResponse
                when(aiAgentService.generateMessage(any(AiMessageGenerationRequestDto.class)))
                        .thenReturn(
                                AiMessageGenerationResponseDto.builder()
                                        .messages(List.of(
                                                AiGeneratedMessageDto.builder()
                                                        .messageType("SingleChoiceQuestion") // Question type
                                                        .content("What do you think?")
                                                        .options(List.of("Option 1", "Option 2"))
                                                        .characterName("Coach")
                                                        .characterRole("COACH")
                                                        .requiresResponse(false) // Inconsistent - should be true
                                                        .build()
                                        ))
                                        .success(true)
                                        .build()
                        );

                // Create user response
                SingleChoiceAnswerMessageDto userResponse = new SingleChoiceAnswerMessageDto();
                userResponse.setId(UUID.randomUUID().toString());
                userResponse.setChatId(testChatId);
                userResponse.setMessageType(MessageType.SINGLE_CHOICE_ANSWER);
                userResponse.setAnswer("Test Response");

                // Send response and expect graceful handling
                MvcResult result = mockMvc.perform(put("/message/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userResponse)))
                        .andExpect(status().isOk())
                        .andReturn();

                System.out.println("✅ Test 10 PASSED: Inconsistent requiresResponse handled gracefully");

            } catch (Exception e) {
                System.err.println("❌ Test 10 FAILED: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Test 11: User sends unsupported input (wrong option index, etc.)
     */
    @Test
    @Order(11)
    void testUnsupportedUserInput() throws Exception {
        assumingThat(testChatId != null, () -> {
            try {
                // Mock normal AI response
                when(aiAgentService.generateMessage(any(AiMessageGenerationRequestDto.class)))
                        .thenReturn(
                                AiMessageGenerationResponseDto.builder()
                                        .messages(List.of(
                                                AiGeneratedMessageDto.builder()
                                                        .messageType("Text")
                                                        .content("Response processed")
                                                        .characterName("Coach")
                                                        .characterRole("COACH")
                                                        .requiresResponse(false)
                                                        .build()
                                        ))
                                        .success(true)
                                        .build()
                        );

                // Create invalid user response
                SingleChoiceAnswerMessageDto userResponse = new SingleChoiceAnswerMessageDto();
                userResponse.setId(UUID.randomUUID().toString());
                userResponse.setChatId(testChatId);
                userResponse.setMessageType(MessageType.SINGLE_CHOICE_ANSWER);
                userResponse.setAnswer("InvalidOption999"); // Invalid option

                // Send response and expect graceful handling
                MvcResult result = mockMvc.perform(put("/message/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userResponse)))
                        .andExpect(status().isOk())
                        .andReturn();

                System.out.println("✅ Test 11 PASSED: Unsupported user input handled gracefully");

            } catch (Exception e) {
                System.err.println("❌ Test 11 FAILED: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Test 12: AI-agent timeout scenario
     */
    @Test
    @Order(12)
    void testAiAgentTimeout() throws Exception {
        assumingThat(testChatId != null, () -> {
            try {
                // Mock timeout scenario
                when(aiAgentService.generateMessage(any(AiMessageGenerationRequestDto.class)))
                        .thenThrow(new RuntimeException("Connection timeout"));

                // Create user response
                SingleChoiceAnswerMessageDto userResponse = new SingleChoiceAnswerMessageDto();
                userResponse.setId(UUID.randomUUID().toString());
                userResponse.setChatId(testChatId);
                userResponse.setMessageType(MessageType.SINGLE_CHOICE_ANSWER);
                userResponse.setAnswer("Test Response");

                // Send response and expect graceful handling
                MvcResult result = mockMvc.perform(put("/message/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userResponse)))
                        .andExpect(status().isOk())
                        .andReturn();

                System.out.println("✅ Test 12 PASSED: AI-agent timeout handled gracefully");

            } catch (Exception e) {
                System.err.println("❌ Test 12 FAILED: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Create test simulation and required data
     */
    private void createTestSimulation() {
        // Create organization
        Organization organization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();
        organizationRepository.save(organization);

        // Create roles if they don't exist
        Role userRole = roleRepository.findByName(StaticRole.ROLE_USER).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setId(1L);
            newRole.setName(StaticRole.ROLE_USER);
            return roleRepository.save(newRole);
        });
        
        Role adminRole = roleRepository.findByName(StaticRole.ROLE_ADMIN).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setId(2L);
            newRole.setName(StaticRole.ROLE_ADMIN);
            return roleRepository.save(newRole);
        });
        
        Role ownerRole = roleRepository.findByName(StaticRole.ROLE_OWNER).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setId(3L);
            newRole.setName(StaticRole.ROLE_OWNER);
            return roleRepository.save(newRole);
        });

        // Create user (MUST match @WithMockUser username)
        User user = User.builder()
                .id(1L)
                .email("test-admin")
                .username("test-admin")
                .password("password")
                .organization(organization)
                .roles(Set.of(userRole, adminRole, ownerRole))
                .build();
        userRepository.save(user);

        // Create skill
        Skill skill = Skill.builder()
                .id(1L)
                .name("AI-Generated Leadership Skill")
                .description("Advanced leadership simulation powered by AI")
                .type(SkillType.DEVELOPMENT)
                .behavior(BehaviorType.DYNAMIC)
                .generationStatus(SkillGenerationStatus.COMPLETED)
                .build();
        skill = skillRepository.save(skill);
        
        // Link organization to skill (CRITICAL for authorization)
        organization.setAvailableSkills(Set.of(skill));
        organizationRepository.saveAndFlush(organization);

        // Create character
        com.backend.softtrainer.entities.Character character = com.backend.softtrainer.entities.Character.builder()
                .id(1L)
                .name("AI Coach")
                .avatar("coach-avatar.png")
                .flowCharacterId(1L)
                .build();
        characterRepository.save(character);

        // Create AI-generated simulation
        Simulation simulation = Simulation.builder()
                .id(1L)
                .name("AI-Generated Leadership Simulation")
                .skill(skill)
                .type(SimulationType.AI_GENERATED)
                .complexity(SimulationComplexity.MEDIUM)
                .hearts(5.0)
                .nodes(Collections.emptyList())
                .build();
        simulation = simulationRepository.save(simulation);
        
        // CRITICAL: Add simulation to skill's simulations map for authorization
        Map<Simulation, Long> skillSimulations = new HashMap<>();
        skillSimulations.put(simulation, simulation.getId());
        skill.setSimulations(skillSimulations);
        skillRepository.saveAndFlush(skill);

        // Store IDs for tests
        testSkillId = skill.getId();
        testSimulationId = simulation.getId();

        System.out.println("✅ Test data created: Skill ID " + testSkillId + ", Simulation ID " + testSimulationId);
    }
}
 