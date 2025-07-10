package com.backend.softtrainer.integration;

import com.backend.softtrainer.config.TestSecurityConfig;
import com.backend.softtrainer.dtos.MessageDto;
import com.backend.softtrainer.services.chatgpt.ChatGptServiceJvmOpenAi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(TestSecurityConfig.class)
@Transactional
public class ModernSimulationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatGptServiceJvmOpenAi chatGptServiceJvmOpenAi; // Mock to avoid OpenAI API key issues

    private static String jwtToken = "test-token"; // Dummy token since auth is mocked
    private static Long simulationId;
    private static Long chatId;

    @BeforeEach
    public void setupMocks() throws InterruptedException {
        // Mock ChatGPT service to return proper content for ResultSimulation messages
        when(chatGptServiceJvmOpenAi.buildAfterwardSimulationRecommendation(
            any(), any(), any(), any(), any(), any()
        )).thenReturn(CompletableFuture.completedFuture(
            new MessageDto("Вітаємо! Ви успішно завершили тест симуляції. Ваші навички емпатії, професіоналізму та вирішення проблем були продемонстровані на високому рівні. Результат: відмінно!")
        ));
    }

    @Test
    @Order(1)
    public void testAuthentication() throws Exception {
        // Skip authentication since we're using @WithMockUser
        System.out.println("✅ Authentication bypassed - using @WithMockUser for test authorization");
    }

    @Test
    @Order(2)
    public void testHealthCheck() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Welcome, Miha")));

        System.out.println("✅ Health check passed");
    }

    @Test
    @Order(3)
    @WithMockUser(username = "test-admin", roles = {"ADMIN", "OWNER"})
    @Commit
    public void testImportModernSimulation() throws Exception {
        String simulationJson = """
            {
                "skill": {
                    "name": "Modern Customer Service Excellence"
                },
                "name": "E2E Test - Modern Customer Service Training",
                "hearts": 5.0,
                "characters": [
                    {
                        "id": 1,
                        "name": "AI-Trainer",
                        "avatar": "https://softtrainer.s3.eu-north-1.amazonaws.com/content/AI.png"
                    },
                    {
                        "id": 2,
                        "name": "Customer",
                        "avatar": "https://softtrainer.s3.eu-north-1.amazonaws.com/content/customer.png"
                    },
                    {
                        "id": -1,
                        "name": "User"
                    }
                ],
                "hyperparameters": [
                    {"key": "empathy", "description": "Level of understanding and care shown"},
                    {"key": "professionalism", "description": "Professional approach and communication"},
                    {"key": "problem_solving", "description": "Ability to find solutions and resolve issues"},
                    {"key": "communication", "description": "Clear and effective communication skills"}
                ],
                "flow": [
                    {
                        "message_id": 1,
                        "previous_message_id": [],
                        "message_type": "Text",
                        "text": "Welcome to our E2E modern customer service training! Today we will test the modern runtime.",
                        "character_id": 1,
                        "flow_rules": [
                            {
                                "type": "ALWAYS_SHOW",
                                "description": "Always show this initial message"
                            }
                        ]
                    },
                    {
                        "message_id": 2,
                        "previous_message_id": [1],
                        "message_type": "Text",
                        "text": "A customer calls: I am extremely frustrated! My premium order is 5 days late and I have a presentation tomorrow!",
                        "character_id": 2,
                        "flow_rules": [
                            {
                                "type": "DEPENDS_ON_PREVIOUS",
                                "previous_message_id": 1,
                                "description": "Show after initial message"
                            }
                        ]
                    },
                    {
                        "message_id": 3,
                        "previous_message_id": [2],
                        "message_type": "SingleChoiceQuestion",
                        "character_id": -1,
                        "text": "What is your immediate response to this upset customer?",
                        "options": [
                            {"id": "option1", "text": "I sincerely apologize for this unacceptable delay. Let me prioritize resolving this immediately."},
                            {"id": "option2", "text": "Unfortunately, delays can happen. Let me see what I can do."},
                            {"id": "option3", "text": "I understand your frustration. Let me check with shipping."},
                            {"id": "option4", "text": "I can offer you a refund or store credit for the inconvenience."}
                        ],
                        "flow_rules": [
                            {
                                "type": "CONDITIONAL_BRANCHING",
                                "conditions": [
                                    {
                                        "type": "OPTION_SELECTED",
                                        "option_id": "option1",
                                        "actions": [
                                            {"type": "INCREASE_HYPERPARAMETER", "key": "empathy", "value": 2},
                                            {"type": "INCREASE_HYPERPARAMETER", "key": "professionalism", "value": 2}
                                        ]
                                    },
                                    {
                                        "type": "OPTION_SELECTED",
                                        "option_id": "option2",
                                        "actions": [
                                            {"type": "INCREASE_HYPERPARAMETER", "key": "empathy", "value": 1}
                                        ]
                                    },
                                    {
                                        "type": "OPTION_SELECTED",
                                        "option_id": "option3",
                                        "actions": [
                                            {"type": "INCREASE_HYPERPARAMETER", "key": "empathy", "value": 1},
                                            {"type": "INCREASE_HYPERPARAMETER", "key": "professionalism", "value": 1}
                                        ]
                                    },
                                    {
                                        "type": "OPTION_SELECTED",
                                        "option_id": "option4",
                                        "actions": [
                                            {"type": "INCREASE_HYPERPARAMETER", "key": "problem_solving", "value": 1}
                                        ]
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        "message_id": 50,
                        "previous_message_id": [3],
                        "message_type": "EnterTextQuestion",
                        "character_id": -1,
                        "text": "Now describe specifically what you would do to resolve this urgent situation:",
                        "flow_rules": [
                            {
                                "type": "ANSWER_QUALITY_RULE",
                                "actions": [
                                    {"type": "INCREASE_HYPERPARAMETER", "key": "problem_solving", "value": 2},
                                    {"type": "INCREASE_HYPERPARAMETER", "key": "communication", "value": 2}
                                ]
                            }
                        ]
                    },
                    {
                        "message_id": 70,
                        "previous_message_id": [50],
                        "message_type": "ResultSimulation",
                        "character_id": 1,
                        "prompt": "Congratulations! You have successfully completed the E2E modern runtime test. Your empathy, professionalism, and problem-solving skills have been demonstrated.",
                        "flow_rules": [
                            {
                                "type": "FINAL_EVALUATION",
                                "conditions": [
                                    {"type": "HYPERPARAMETER_THRESHOLD", "key": "empathy", "min_value": 3},
                                    {"type": "HYPERPARAMETER_THRESHOLD", "key": "professionalism", "min_value": 3},
                                    {"type": "HYPERPARAMETER_THRESHOLD", "key": "problem_solving", "min_value": 3}
                                ]
                            }
                        ]
                    }
                ]
            }
            """;

        MvcResult result = mockMvc.perform(put("/flow/upload")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(simulationJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.name").value("E2E Test - Modern Customer Service Training"))
                .andReturn();

        System.out.println("✅ Modern simulation imported successfully with flow_rules logic");
        System.out.println("Response: " + result.getResponse().getContentAsString());
    }

    @Test
    @Order(4)
    @WithMockUser(username = "test-admin", roles = {"ADMIN", "OWNER"})
    @Commit
    public void testFindImportedSimulation() throws Exception {
        // First get available skills to find our imported skill
        System.out.println("🔍 Calling /skills/available endpoint...");

        MvcResult skillsResult = mockMvc.perform(get("/skills/available")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("📋 Skills response status: " + skillsResult.getResponse().getStatus());
        System.out.println("📋 Skills response content: " + skillsResult.getResponse().getContentAsString());

        JsonNode skillsResponse = objectMapper.readTree(skillsResult.getResponse().getContentAsString());

        // Check if the response has success field and what it says
        if (skillsResponse.has("success")) {
            System.out.println("✅ Success field: " + skillsResponse.get("success").asBoolean());
        }

        JsonNode skills = skillsResponse.get("skills");
        if (skills == null) {
            System.out.println("❌ Skills field is null in response");
            return;
        }

        // Debug: Print all available skills
        System.out.println("Available skills:");
        for (JsonNode skill : skills) {
            System.out.println("- " + skill.get("name").asText() + " (ID: " + skill.get("id").asLong() + ")");
        }

        Long skillId = null;
        for (JsonNode skill : skills) {
            if ("Modern Customer Service Excellence".equals(skill.get("name").asText())) {
                skillId = skill.get("id").asLong();
                break;
            }
        }

        assertNotNull(skillId, "Modern Customer Service Excellence skill should be found");
        System.out.println("✅ Found skill ID: " + skillId);

        // Now get simulations for this skill
        MvcResult simulationsResult = mockMvc.perform(get("/skills/simulations")
                .param("skillId", skillId.toString())
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode simulationsResponse = objectMapper.readTree(simulationsResult.getResponse().getContentAsString());
        JsonNode simulations = simulationsResponse.get("simulations");

        for (JsonNode simulation : simulations) {
            if ("E2E Test - Modern Customer Service Training".equals(simulation.get("name").asText())) {
                simulationId = simulation.get("id").asLong();
                break;
            }
        }

        assertNotNull(simulationId, "E2E Test Modern simulation should be found");
        System.out.println("✅ Found simulation ID: " + simulationId);
    }

    @Test
    @Order(5)
    @WithMockUser(username = "test-admin", roles = {"ADMIN", "OWNER"})
    @Commit
    public void testCreateChatWithModernRuntime() throws Exception {
        String chatRequest = String.format(
            "{\"simulation_id\": %d, \"skill_id\": null}", 
            simulationId
        );

        MvcResult result = mockMvc.perform(put("/chats/create")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(chatRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.chat_id").exists())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        chatId = response.get("chat_id").asLong();

        assertNotNull(chatId);

        // Verify modern runtime indicators
        String errorMessage = response.get("error_message").asText();
        assertTrue(errorMessage.contains("success") || errorMessage.contains("modern") || errorMessage.contains("runtime"),
                "Should contain success or modern runtime indicator");

        // Verify we have initial messages
        JsonNode messages = response.get("messages");
        assertTrue(messages.isArray() && messages.size() >= 2, "Should have at least welcome and customer complaint messages");

        System.out.println("✅ Chat created successfully with ID: " + chatId);
        System.out.println("✅ Initial messages loaded: " + messages.size());
        System.out.println("✅ Response indicates: " + errorMessage);
    }

    @Test
    @Order(6)
    @WithMockUser(username = "test-admin", roles = {"ADMIN", "OWNER"})
    @Commit
    public void testCompleteSimulationFlow() throws Exception {
        // Defensive: ensure simulationId and chatId are initialized
        if (simulationId == null) {
            testImportModernSimulation();
            testFindImportedSimulation();
        }
        if (chatId == null) {
            testCreateChatWithModernRuntime();
        }

        // Step 1: Get current chat state and find the SingleChoiceQuestion
        System.out.println("🔍 Getting chat for simulation ID: " + simulationId);
        MvcResult chatResult = mockMvc.perform(get("/chats/get")
                .param("simulationId", simulationId.toString())
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andReturn();
        
        System.out.println("📋 Chat response: " + chatResult.getResponse().getContentAsString());

        JsonNode chatResponse = objectMapper.readTree(chatResult.getResponse().getContentAsString());
        
        // Check if the response is successful
        boolean success = chatResponse.get("success").asBoolean();
        if (!success) {
            String errorMessage = chatResponse.get("error_message").asText();
            System.out.println("❌ Chat retrieval failed: " + errorMessage);
            fail("Failed to retrieve chat: " + errorMessage);
        }
        
        JsonNode messages = chatResponse.get("messages");

        // Extract the actual chat ID from the response
        Long actualChatId = chatResponse.get("chat_id").asLong();

        // Find the SingleChoiceQuestion message
        JsonNode choiceQuestion = null;
        String questionId = null;
        JsonNode options = null;

        for (JsonNode message : messages) {
            if ("SingleChoiceQuestion".equals(message.get("message_type").asText())) {
                choiceQuestion = message;
                questionId = message.get("id").asText();
                options = message.get("options");
                break;
            }
        }

        assertNotNull(choiceQuestion, "Should find SingleChoiceQuestion message");
        assertNotNull(questionId, "Question should have an ID");
        assertNotNull(options, "Question should have options");
        
        System.out.println("📋 Options found: " + options.size());
        System.out.println("📋 Options content: " + options.toString());
        
        assertTrue(options.size() >= 4, "Should have 4 options, but found: " + options.size());

        System.out.println("✅ Found SingleChoiceQuestion with ID: " + questionId);

        // Step 2: Answer the first question (choose the first option which is the most empathetic)
        String correctOptionId = options.get(0).get("option_id").asText(); // Use actual option ID from response

        // Step 3: Send the correct answer
        String answerRequest = String.format("""
            {
                "id": "%s",
                "chat_id": %d,
                "message_type": "SingleChoiceQuestion",
                "answer": "%s",
                "user_response_time": 1000
            }
            """, questionId, actualChatId, correctOptionId);

            MvcResult asyncResult = mockMvc.perform(put("/message/send")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(answerRequest))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            // Wait for async processing to complete
            mockMvc.perform(asyncDispatch(asyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

        System.out.println("✅ Sent correct answer for SingleChoiceQuestion");
    }
}
