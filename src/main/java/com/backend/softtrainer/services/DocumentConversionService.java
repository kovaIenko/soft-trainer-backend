package com.backend.softtrainer.services;

import com.backend.softtrainer.entities.flow.EnhancedFlowNode;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.entities.enums.InteractionType;
import com.backend.softtrainer.services.chatgpt.ChatGptService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * 📄 Document Conversion Service - AI-powered document to simulation converter
 * 
 * This service demonstrates how documents can be automatically converted into 
 * interactive simulations using LLM analysis and content generation.
 * 
 * Supported formats:
 * - PDF documents
 * - Word documents (.docx)
 * - PowerPoint presentations (.pptx)
 * - Plain text files
 * - Web content (URLs)
 */
@Service
@AllArgsConstructor
@Slf4j
public class DocumentConversionService {
    
    private final ChatGptService chatGptService;
    private final ObjectMapper objectMapper;
    
    @Data
    @Builder
    public static class ConversionRequest {
        private MultipartFile document;
        private String documentUrl;
        private String simulationType; // "role_play", "decision_making", "skill_practice"
        private List<String> learningObjectives;
        private String targetAudience;
        private Integer maxMessages;
        private String organizationContext;
    }
    
    @Data
    @Builder
    public static class ConversionResult {
        private List<EnhancedFlowNode> generatedNodes;
        private Map<String, Object> simulationMetadata;
        private List<String> extractedLearningObjectives;
        private String simulationSummary;
        private Double confidenceScore;
        private List<String> recommendations;
    }
    
    /**
     * 🚀 Main conversion method - transforms document into simulation
     */
    public ConversionResult convertDocumentToSimulation(ConversionRequest request) {
        log.info("📄 Starting document conversion to simulation");
        
        try {
            // 1. Extract content from document
            String documentContent = extractDocumentContent(request);
            
            // 2. Analyze content for simulation potential
            ContentAnalysis analysis = analyzeContentForSimulation(documentContent, request);
            
            // 3. Generate simulation structure
            List<EnhancedFlowNode> nodes = generateSimulationNodes(analysis, request);
            
            // 4. Create metadata and recommendations
            Map<String, Object> metadata = generateSimulationMetadata(analysis);
            List<String> recommendations = generateRecommendations(analysis);
            
            return ConversionResult.builder()
                .generatedNodes(nodes)
                .simulationMetadata(metadata)
                .extractedLearningObjectives(analysis.learningObjectives)
                .simulationSummary(analysis.summary)
                .confidenceScore(analysis.confidenceScore)
                .recommendations(recommendations)
                .build();
                
        } catch (Exception e) {
            log.error("❌ Error converting document to simulation", e);
            throw new RuntimeException("Document conversion failed", e);
        }
    }
    
    /**
     * 📖 Extract text content from various document formats
     */
    private String extractDocumentContent(ConversionRequest request) {
        if (request.getDocument() != null) {
            return extractFromFile(request.getDocument());
        } else if (request.getDocumentUrl() != null) {
            return extractFromUrl(request.getDocumentUrl());
        } else {
            throw new IllegalArgumentException("No document or URL provided");
        }
    }
    
    /**
     * 📁 Extract content from uploaded file
     */
    private String extractFromFile(MultipartFile file) {
        log.debug("📁 Extracting content from file: {}", file.getOriginalFilename());
        
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Invalid file");
        }
        
        try {
            if (filename.endsWith(".pdf")) {
                return extractFromPdf(file);
            } else if (filename.endsWith(".docx")) {
                return extractFromWord(file);
            } else if (filename.endsWith(".pptx")) {
                return extractFromPowerPoint(file);
            } else if (filename.endsWith(".txt")) {
                return new String(file.getBytes());
            } else {
                throw new IllegalArgumentException("Unsupported file format: " + filename);
            }
        } catch (Exception e) {
            log.error("❌ Error extracting content from file", e);
            throw new RuntimeException("File extraction failed", e);
        }
    }
    
    /**
     * 🌐 Extract content from URL
     */
    private String extractFromUrl(String url) {
        log.debug("🌐 Extracting content from URL: {}", url);
        
        // Implementation would use web scraping or URL content extraction
        // For now, return placeholder
        return "Content extracted from: " + url;
    }
    
    /**
     * 📄 Extract text from PDF
     */
    private String extractFromPdf(MultipartFile file) {
        // Implementation would use PDF parsing library like Apache PDFBox
        // For now, return placeholder
        return "PDF content extracted";
    }
    
    /**
     * 📝 Extract text from Word document
     */
    private String extractFromWord(MultipartFile file) {
        // Implementation would use Apache POI
        // For now, return placeholder
        return "Word document content extracted";
    }
    
    /**
     * 📊 Extract text from PowerPoint
     */
    private String extractFromPowerPoint(MultipartFile file) {
        // Implementation would use Apache POI
        // For now, return placeholder
        return "PowerPoint content extracted";
    }
    
    @Data
    @Builder
    private static class ContentAnalysis {
        private String summary;
        private List<String> learningObjectives;
        private List<String> keyTopics;
        private List<String> potentialScenarios;
        private String recommendedSimulationType;
        private Double confidenceScore;
        private Map<String, Object> additionalMetadata;
    }
    
    /**
     * 🔍 Analyze document content for simulation conversion potential
     */
    private ContentAnalysis analyzeContentForSimulation(String content, ConversionRequest request) {
        log.debug("🔍 Analyzing content for simulation potential");
        
        String analysisPrompt = buildAnalysisPrompt(content, request);
        
        try {
            // Use AI to analyze the content
            String aiResponse = chatGptService.generateOverview(
                analysisPrompt, 
                "asst_content_analyzer", 
                "gpt-4o-mini"
            );
            
            return parseAnalysisResponse(aiResponse);
            
        } catch (Exception e) {
            log.error("❌ Error in AI content analysis", e);
            return createFallbackAnalysis(content);
        }
    }
    
    /**
     * 📝 Build AI prompt for content analysis
     */
    private String buildAnalysisPrompt(String content, ConversionRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert learning designer analyzing content for simulation conversion.\n\n");
        prompt.append("CONTENT TO ANALYZE:\n");
        prompt.append(content.substring(0, Math.min(content.length(), 3000))); // Limit content size
        prompt.append("\n\n");
        
        prompt.append("CONVERSION REQUIREMENTS:\n");
        prompt.append("- Simulation Type: ").append(request.getSimulationType()).append("\n");
        prompt.append("- Target Audience: ").append(request.getTargetAudience()).append("\n");
        prompt.append("- Learning Objectives: ").append(request.getLearningObjectives()).append("\n\n");
        
        prompt.append("ANALYSIS TASKS:\n");
        prompt.append("1. Extract key learning objectives from the content\n");
        prompt.append("2. Identify potential scenarios for role-playing or decision-making\n");
        prompt.append("3. Suggest interactive elements and conversation flows\n");
        prompt.append("4. Rate the conversion confidence (0.0-1.0)\n");
        prompt.append("5. Provide specific recommendations for the simulation structure\n\n");
        
        prompt.append("RESPONSE FORMAT (JSON):\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"Brief summary of content\",\n");
        prompt.append("  \"learning_objectives\": [\"objective1\", \"objective2\"],\n");
        prompt.append("  \"key_topics\": [\"topic1\", \"topic2\"],\n");
        prompt.append("  \"potential_scenarios\": [\"scenario1\", \"scenario2\"],\n");
        prompt.append("  \"recommended_simulation_type\": \"role_play\",\n");
        prompt.append("  \"confidence_score\": 0.85,\n");
        prompt.append("  \"recommendations\": [\"rec1\", \"rec2\"]\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    /**
     * 📊 Parse AI analysis response
     */
    private ContentAnalysis parseAnalysisResponse(String aiResponse) {
        try {
            JsonNode json = objectMapper.readTree(aiResponse);
            
            return ContentAnalysis.builder()
                .summary(json.get("summary").asText())
                .learningObjectives(parseStringArray(json.get("learning_objectives")))
                .keyTopics(parseStringArray(json.get("key_topics")))
                .potentialScenarios(parseStringArray(json.get("potential_scenarios")))
                .recommendedSimulationType(json.get("recommended_simulation_type").asText())
                .confidenceScore(json.get("confidence_score").asDouble())
                .build();
                
        } catch (Exception e) {
            log.error("❌ Error parsing AI analysis response", e);
            return createFallbackAnalysis(aiResponse);
        }
    }
    
    /**
     * 🆘 Create fallback analysis when AI analysis fails
     */
    private ContentAnalysis createFallbackAnalysis(String content) {
        return ContentAnalysis.builder()
            .summary("Content analysis using fallback method")
            .learningObjectives(List.of("communication", "problem_solving"))
            .keyTopics(List.of("general_content"))
            .potentialScenarios(List.of("general_conversation"))
            .recommendedSimulationType("role_play")
            .confidenceScore(0.5)
            .build();
    }
    
    /**
     * 🏗️ Generate simulation nodes based on content analysis
     */
    private List<EnhancedFlowNode> generateSimulationNodes(ContentAnalysis analysis, ConversionRequest request) {
        log.debug("🏗️ Generating simulation nodes from analysis");
        
        List<EnhancedFlowNode> nodes = new ArrayList<>();
        
        // Generate welcome message
        nodes.add(createWelcomeNode(analysis));
        
        // Generate scenario introduction
        nodes.add(createScenarioNode(analysis));
        
        // Generate interactive conversation nodes
        nodes.addAll(createConversationNodes(analysis, request));
        
        // Generate result/summary node
        nodes.add(createResultNode(analysis));
        
        log.info("✅ Generated {} simulation nodes", nodes.size());
        return nodes;
    }
    
    /**
     * 👋 Create welcome/introduction node
     */
    private EnhancedFlowNode createWelcomeNode(ContentAnalysis analysis) {
        return EnhancedFlowNode.builder()
            .messageId(1L)
            .messageType(MessageType.TEXT)
            .interactionType(InteractionType.TEXT_DISPLAY)
            .text("Welcome to this interactive simulation based on: " + analysis.getSummary())
            .characterIdRaw(1)
            .build();
    }
    
    /**
     * 🎭 Create scenario setup node
     */
    private EnhancedFlowNode createScenarioNode(ContentAnalysis analysis) {
        String scenarioText = analysis.getPotentialScenarios().isEmpty() ? 
            "Let's explore this scenario together." :
            "Scenario: " + analysis.getPotentialScenarios().get(0);
            
        return EnhancedFlowNode.builder()
            .messageId(10L)
            .messageType(MessageType.SINGLE_CHOICE_QUESTION)
            .interactionType(InteractionType.SINGLE_CHOICE)
            .text(scenarioText)
            .characterIdRaw(1)
            .build();
    }
    
    /**
     * 💬 Create conversation flow nodes
     */
    private List<EnhancedFlowNode> createConversationNodes(ContentAnalysis analysis, ConversionRequest request) {
        List<EnhancedFlowNode> nodes = new ArrayList<>();
        
        // Generate nodes based on learning objectives
        for (int i = 0; i < analysis.getLearningObjectives().size(); i++) {
            String objective = analysis.getLearningObjectives().get(i);
            
            EnhancedFlowNode node = EnhancedFlowNode.builder()
                .messageId(20L + i)
                .messageType(MessageType.SINGLE_CHOICE_QUESTION)
                .interactionType(InteractionType.SINGLE_CHOICE)
                .text("How would you approach this situation to demonstrate " + objective + "?")
                .characterIdRaw(1)
                .build();
                
            nodes.add(node);
        }
        
        return nodes;
    }
    
    /**
     * 🏁 Create result/summary node
     */
    private EnhancedFlowNode createResultNode(ContentAnalysis analysis) {
        return EnhancedFlowNode.builder()
            .messageId(100L)
            .messageType(MessageType.RESULT_SIMULATION)
            .interactionType(InteractionType.RESULT_SUMMARY)
            .prompt("Excellent work! You've completed the simulation based on: " + analysis.getSummary())
            .characterIdRaw(1)
            .build();
    }
    
    /**
     * 📊 Generate simulation metadata
     */
    private Map<String, Object> generateSimulationMetadata(ContentAnalysis analysis) {
        return Map.of(
            "source", "document_conversion",
            "confidence_score", analysis.getConfidenceScore(),
            "learning_objectives", analysis.getLearningObjectives(),
            "topics", analysis.getKeyTopics(),
            "recommended_type", analysis.getRecommendedSimulationType()
        );
    }
    
    /**
     * 💡 Generate recommendations for improvement
     */
    private List<String> generateRecommendations(ContentAnalysis analysis) {
        List<String> recommendations = new ArrayList<>();
        
        if (analysis.getConfidenceScore() < 0.7) {
            recommendations.add("Consider providing more specific learning objectives");
            recommendations.add("Add more interactive scenarios to the content");
        }
        
        if (analysis.getLearningObjectives().size() < 3) {
            recommendations.add("Consider expanding the learning objectives");
        }
        
        recommendations.add("Review generated scenarios for accuracy and relevance");
        recommendations.add("Test the simulation with target audience before deployment");
        
        return recommendations;
    }
    
    /**
     * 🔧 Helper method to parse string arrays from JSON
     */
    private List<String> parseStringArray(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            arrayNode.forEach(item -> result.add(item.asText()));
        }
        return result;
    }
} 