package com.backend.softtrainer.simulation.strategies;

import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.enums.SimulationMode;
import com.backend.softtrainer.simulation.ContentStrategy;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.services.chatgpt.ChatGptService;
import com.backend.softtrainer.services.UserHyperParameterService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 🎯 Targeted AI Content Strategy - Generates messages focused on specific learning objectives
 * 
 * This strategy uses LLM to create the next few messages based on:
 * - Current user skill levels (hyperparameters)
 * - Specific learning objectives to measure/train
 * - Educational best practices for skill development
 */
@Service
@AllArgsConstructor
@Slf4j
public class TargetedAiContentStrategy implements ContentStrategy {
    
    private final ChatGptService chatGptService;
    private final UserHyperParameterService userHyperParameterService;
    
    @Override
    public SimulationMode getMode() {
        return SimulationMode.DYNAMIC;
    }
    
    @Override
    public boolean canHandle(SimulationContext context) {
        return context.getSimulationMode() == SimulationMode.DYNAMIC;
    }
    
    @Override
    public boolean isSimulationComplete(SimulationContext context) {
        return context.isMarkedAsCompleted() || context.getMessageCount() > 50;
    }
    
    @Data
    @Builder
    public static class AiContentRequest {
        private List<String> learningObjectives;
        private Integer nextMessageCount;
        private SimulationContext context;
        private String specificFocus;
        private Double difficultyLevel; // 1.0 = basic, 5.0 = advanced
    }
    
    @Override
    public CompletableFuture<List<Message>> generateResponse(SimulationContext context, Message userMessage) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Analyze user response to determine next learning focus
                AiContentRequest request = analyzeUserResponse(context, userMessage);
                
                // Generate targeted messages
                return generateTargetedMessages(request);
                
            } catch (Exception e) {
                log.error("❌ Error in targeted AI content generation: {}", e.getMessage());
                return List.of();
            }
        });
    }
    
    @Override
    public CompletableFuture<List<Message>> initializeSimulation(SimulationContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Generate initial assessment messages
                AiContentRequest request = AiContentRequest.builder()
                    .learningObjectives(context.getSimulationLearningObjectives())
                    .nextMessageCount(2)
                    .context(context)
                    .specificFocus("initial_assessment")
                    .difficultyLevel(1.0)
                    .build();
                
                return generateTargetedMessages(request);
                
            } catch (Exception e) {
                log.error("❌ Error initializing targeted AI simulation: {}", e.getMessage());
                return List.of();
            }
        });
    }
    
    /**
     * 🎯 Generate messages targeted at specific learning objectives
     */
    public List<Message> generateTargetedMessages(AiContentRequest request) {
        log.debug("🎯 Generating {} targeted messages for objectives: {}", 
            request.getNextMessageCount(), request.getLearningObjectives());
        
        String prompt = buildEducationalPrompt(request);
        
        try {
            // TODO: Implement AI response generation using ChatGptService
            // String aiResponse = chatGptService.generateOverview(prompt, "assistant_id", "gpt-4o-mini");
            
            // For now, return placeholder
            log.debug("🚧 AI content generation not yet implemented, returning placeholder");
            return List.of();
            
        } catch (Exception e) {
            log.error("❌ Error generating targeted AI messages: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * 📝 Build educational prompt focused on learning objectives
     */
    private String buildEducationalPrompt(AiContentRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert learning designer creating interactive simulation content.\n\n");
        
        prompt.append("LEARNING OBJECTIVES TO MEASURE/TRAIN:\n");
        request.getLearningObjectives().forEach(objective -> 
            prompt.append("- ").append(objective).append("\n"));
        
        prompt.append("\nCURRENT USER SKILL LEVELS:\n");
        request.getLearningObjectives().forEach(objective -> {
            Double level = request.getContext().getHyperParameter(objective);
            prompt.append("- ").append(objective).append(": ").append(level != null ? level : 0.0).append("/10\n");
        });
        
        prompt.append("\nGENERATE: Create ").append(request.getNextMessageCount())
               .append(" messages that will help measure and improve these specific skills.\n");
        
        prompt.append("\nFOCUS: ").append(request.getSpecificFocus()).append("\n");
        prompt.append("DIFFICULTY: ").append(request.getDifficultyLevel()).append("/5.0\n\n");
        
        prompt.append("REQUIREMENTS:\n");
        prompt.append("1. Each message should target specific learning objectives\n");
        prompt.append("2. Include clear measurement opportunities\n");
        prompt.append("3. Progressive difficulty based on user levels\n");
        prompt.append("4. Engaging, realistic scenarios\n");
        prompt.append("5. Format as JSON array with message structure\n\n");
        
        prompt.append("Return JSON format:\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"message_type\": \"SingleChoiceQuestion\",\n");
        prompt.append("    \"text\": \"Message content...\",\n");
        prompt.append("    \"options\": [\"Option 1\", \"Option 2\", \"Option 3\"],\n");
        prompt.append("    \"learning_focus\": [\"objective1\", \"objective2\"],\n");
        prompt.append("    \"measurement_intent\": \"What this measures\"\n");
        prompt.append("  }\n");
        prompt.append("]\n");
        
        return prompt.toString();
    }
    
    /**
     * 🔍 Analyze user response to determine next learning focus
     */
    private AiContentRequest analyzeUserResponse(SimulationContext context, Message userMessage) {
        // Simple analysis for now - could be enhanced with AI
        List<String> objectives = context.getSimulationLearningObjectives();
        
        // Determine difficulty based on current performance
        Double avgLevel = objectives.stream()
            .mapToDouble(obj -> context.getHyperParameter(obj))
            .average()
            .orElse(1.0);
        
        return AiContentRequest.builder()
            .learningObjectives(objectives)
            .nextMessageCount(2)
            .context(context)
            .specificFocus("skill_development")
            .difficultyLevel(Math.min(5.0, avgLevel / 2.0 + 1.0))
            .build();
    }
    
    /**
     * 📋 Parse AI response into Message objects
     */
    private List<Message> parseAiResponseToMessages(String aiResponse, AiContentRequest request) {
        // TODO: Implement JSON parsing to Message objects
        // For now, return empty list
        log.debug("📋 Parsing AI response: {}", aiResponse.substring(0, Math.min(100, aiResponse.length())));
        return List.of();
    }
} 