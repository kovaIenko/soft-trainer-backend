package com.backend.softtrainer.services.content;

import com.backend.softtrainer.entities.enums.TextFormat;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 📝 Rich Text Processor - Handles advanced text formatting and rendering
 * 
 * Supports:
 * - Markdown to HTML conversion
 * - Dynamic variable substitution
 * - Interactive element embedding
 * - Context-aware content adaptation
 */
@Service
@Slf4j
public class RichTextProcessor {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Regex patterns for text processing
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private static final Pattern MARKDOWN_BOLD_PATTERN = Pattern.compile("\\*\\*([^*]+)\\*\\*");
    private static final Pattern MARKDOWN_ITALIC_PATTERN = Pattern.compile("\\*([^*]+)\\*");
    private static final Pattern MARKDOWN_CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern INTERACTIVE_ELEMENT_PATTERN = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");
    
    /**
     * 🎨 Process text content based on specified format
     */
    public String processText(String rawText, TextFormat format, SimulationContext context) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return rawText;
        }
        
        log.debug("📝 Processing text with format: {} for context: {}", format, context.getChatId());
        
        return switch (format) {
            case PLAIN_TEXT -> processPlainText(rawText, context);
            case MARKDOWN -> processMarkdown(rawText, context);
            case HTML -> processHtml(rawText, context);
            case FORMATTED_JSON -> processFormattedJson(rawText, context);
            case INTERACTIVE_TEXT -> processInteractiveText(rawText, context);
        };
    }
    
    /**
     * 📄 Process plain text with variable substitution
     */
    private String processPlainText(String text, SimulationContext context) {
        return substituteVariables(text, context);
    }
    
    /**
     * 📋 Process markdown text and convert to HTML
     */
    private String processMarkdown(String text, SimulationContext context) {
        String processedText = substituteVariables(text, context);
        
        // Convert markdown elements to HTML
        processedText = MARKDOWN_BOLD_PATTERN.matcher(processedText)
            .replaceAll("<strong>$1</strong>");
        processedText = MARKDOWN_ITALIC_PATTERN.matcher(processedText)
            .replaceAll("<em>$1</em>");
        processedText = MARKDOWN_CODE_PATTERN.matcher(processedText)
            .replaceAll("<code>$1</code>");
        
        // Handle headers
        processedText = processedText.replaceAll("^### (.+)$", "<h3>$1</h3>");
        processedText = processedText.replaceAll("^## (.+)$", "<h2>$1</h2>");
        processedText = processedText.replaceAll("^# (.+)$", "<h1>$1</h1>");
        
        // Handle line breaks
        processedText = processedText.replaceAll("\n", "<br>");
        
        return processedText;
    }
    
    /**
     * 🎨 Process HTML content with security validation
     */
    private String processHtml(String text, SimulationContext context) {
        String processedText = substituteVariables(text, context);
        
        // Basic HTML sanitization (in production, use a proper HTML sanitizer)
        processedText = sanitizeHtml(processedText);
        
        return processedText;
    }
    
    /**
     * 📊 Process formatted JSON content
     */
    private String processFormattedJson(String text, SimulationContext context) {
        try {
            JsonNode jsonNode = objectMapper.readTree(text);
            return processJsonContent(jsonNode, context);
        } catch (Exception e) {
            log.warn("⚠️ Failed to process JSON content: {}", e.getMessage());
            return processPlainText(text, context);
        }
    }
    
    /**
     * 🔧 Process interactive text with embedded elements
     */
    private String processInteractiveText(String text, SimulationContext context) {
        String processedText = substituteVariables(text, context);
        
        // Process interactive elements
        Matcher matcher = INTERACTIVE_ELEMENT_PATTERN.matcher(processedText);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String elementSpec = matcher.group(1);
            String replacement = createInteractiveElement(elementSpec, context);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 🔄 Substitute context variables in text
     */
    private String substituteVariables(String text, SimulationContext context) {
        if (context == null) {
            return text;
        }
        
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = getContextVariable(variableName, context);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 📊 Get variable value from simulation context
     */
    private String getContextVariable(String variableName, SimulationContext context) {
        return switch (variableName.toLowerCase()) {
            case "user_name" -> context.getUser() != null ? context.getUser().getName() : "User";
            case "hearts" -> String.valueOf(context.getHearts().intValue());
            case "message_count" -> String.valueOf(context.getMessageCount());
            case "skill_name" -> context.getSkillName();
            case "duration" -> String.valueOf(context.getDurationSeconds());
            default -> {
                // Try to get hyperparameter
                if (variableName.startsWith("param_")) {
                    String paramName = variableName.substring(6);
                    yield String.valueOf(context.getHyperParameter(paramName).intValue());
                }
                yield "{{" + variableName + "}}"; // Keep original if not found
            }
        };
    }
    
    /**
     * 🔧 Create interactive element from specification
     */
    private String createInteractiveElement(String elementSpec, SimulationContext context) {
        String[] parts = elementSpec.split(":");
        if (parts.length < 2) {
            return elementSpec;
        }
        
        String elementType = parts[0].trim();
        String elementValue = parts[1].trim();
        
        return switch (elementType.toLowerCase()) {
            case "progress" -> createProgressBar(elementValue, context);
            case "badge" -> createBadge(elementValue, context);
            case "tooltip" -> createTooltip(elementValue, context);
            case "highlight" -> createHighlight(elementValue, context);
            default -> elementSpec;
        };
    }
    
    /**
     * 📊 Create progress bar element
     */
    private String createProgressBar(String spec, SimulationContext context) {
        try {
            String[] parts = spec.split(",");
            String paramName = parts[0].trim();
            int maxValue = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 10;
            
            double currentValue = context.getHyperParameter(paramName);
            int percentage = (int) ((currentValue / maxValue) * 100);
            
            return String.format(
                "<div class=\"progress-bar\" data-param=\"%s\" data-value=\"%.1f\" data-max=\"%d\" style=\"width: %d%%\">%s: %.1f/%d</div>",
                paramName, currentValue, maxValue, percentage, paramName, currentValue, maxValue
            );
        } catch (Exception e) {
            return spec;
        }
    }
    
    /**
     * 🏆 Create badge element
     */
    private String createBadge(String spec, SimulationContext context) {
        String[] parts = spec.split(",");
        String badgeText = parts[0].trim();
        String badgeType = parts.length > 1 ? parts[1].trim() : "info";
        
        return String.format("<span class=\"badge badge-%s\">%s</span>", badgeType, badgeText);
    }
    
    /**
     * 💡 Create tooltip element
     */
    private String createTooltip(String spec, SimulationContext context) {
        String[] parts = spec.split(",", 2);
        String text = parts[0].trim();
        String tooltip = parts.length > 1 ? parts[1].trim() : "";
        
        return String.format("<span class=\"tooltip\" title=\"%s\">%s</span>", tooltip, text);
    }
    
    /**
     * ✨ Create highlight element
     */
    private String createHighlight(String spec, SimulationContext context) {
        String[] parts = spec.split(",");
        String text = parts[0].trim();
        String color = parts.length > 1 ? parts[1].trim() : "yellow";
        
        return String.format("<mark style=\"background-color: %s\">%s</mark>", color, text);
    }
    
    /**
     * 📊 Process JSON content structure
     */
    private String processJsonContent(JsonNode jsonNode, SimulationContext context) {
        if (jsonNode.has("template")) {
            String template = jsonNode.get("template").asText();
            JsonNode data = jsonNode.get("data");
            return applyJsonTemplate(template, data, context);
        }
        
        return jsonNode.toString();
    }
    
    /**
     * 📋 Apply JSON template with data
     */
    private String applyJsonTemplate(String template, JsonNode data, SimulationContext context) {
        // Simple template processing
        String result = template;
        
        if (data != null && data.isObject()) {
            data.fields().forEachRemaining(entry -> {
                String placeholder = "{{" + entry.getKey() + "}}";
                result.replace(placeholder, entry.getValue().asText());
            });
        }
        
        return substituteVariables(result, context);
    }
    
    /**
     * 🛡️ Basic HTML sanitization
     */
    private String sanitizeHtml(String html) {
        // Basic sanitization - in production use OWASP HTML Sanitizer
        return html
            .replaceAll("<script[^>]*>.*?</script>", "")
            .replaceAll("javascript:", "")
            .replaceAll("on\\w+=\"[^\"]*\"", "");
    }
    
    /**
     * 🎨 Generate formatted text for display
     */
    public String generateFormattedContent(String content, Map<String, Object> variables) {
        String result = content;
        
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, String.valueOf(entry.getValue()));
        }
        
        return result;
    }
    
    /**
     * 📊 Create summary statistics display
     */
    public String createStatsSummary(SimulationContext context) {
        return String.format("""
            <div class="stats-summary">
                <div class="stat-item">
                    <span class="stat-label">Hearts:</span>
                    <span class="stat-value">%.1f</span>
                </div>
                <div class="stat-item">
                    <span class="stat-label">Messages:</span>
                    <span class="stat-value">%d</span>
                </div>
                <div class="stat-item">
                    <span class="stat-label">Duration:</span>
                    <span class="stat-value">%d seconds</span>
                </div>
            </div>
            """, 
            context.getHearts(), 
            context.getMessageCount(), 
            context.getDurationSeconds()
        );
    }
} 