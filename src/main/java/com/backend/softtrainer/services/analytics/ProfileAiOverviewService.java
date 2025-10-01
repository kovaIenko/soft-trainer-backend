package com.backend.softtrainer.services.analytics;

import com.backend.softtrainer.entities.AiOverview;
import com.backend.softtrainer.entities.Prompt;
import com.backend.softtrainer.entities.PromptName;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.AiOverviewRepository;
import com.backend.softtrainer.repositories.PromptRepository;
import com.backend.softtrainer.repositories.UserHyperParameterRepository;
import com.backend.softtrainer.repositories.UserRepository;
import com.backend.softtrainer.services.chatgpt.ChatGptService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class ProfileAiOverviewService {
  private final UserRepository userRepository;
  private final AiOverviewRepository aiOverviewRepository;
  private final PromptRepository promptRepository;
  private final UserHyperParameterRepository userHyperParameterRepository;
  private final ChatGptService chatGptService;
  private final ProfileAnalyticsService profileAnalyticsService;

  private static final String PROFILE_ENTITY_TYPE = "PROFILE";
  private static final String LLM_MODEL = "gpt-3.5-turbo";
  private static final PromptName PROMPT_NAME = PromptName.PROFILE_AI_OVERVIEW;
  private static final int MAX_SIMULATIONS_FOR_OVERVIEW = 3; // Limit to last 3 simulations for AI overview
  private static final int MAX_PROMPT_LENGTH = 255000; // OpenAI limit is 256k, keep buffer

  public Optional<AiOverview> getLatestProfileOverview(Long userId) {
    return aiOverviewRepository.findLatestByEntity(PROFILE_ENTITY_TYPE, userId);
  }

  public AiOverview generateAndStoreProfileOverview(User user) {
    Prompt prompt = promptRepository.findFirstByNameOrderByIdDesc(PROMPT_NAME)
      .orElseThrow(() -> new RuntimeException("Prompt for profile AI overview not found"));

    log.info("[AI Overview] Prompt template length: {} characters", prompt.getPrompt().length());
    log.debug("[AI Overview] Prompt template:\n{}", prompt.getPrompt());
    
    // Start with configured max simulations and reduce if needed
    int simulationsToInclude = MAX_SIMULATIONS_FOR_OVERVIEW;
    String filledPrompt;
    Map<String, Object> analytics;
    
    // Iteratively reduce data until prompt fits
    do {
      analytics = ProfileAiOverviewUtil.collectUserAnalytics(user, profileAnalyticsService, simulationsToInclude);
      log.info("[AI Overview] Analytics collected with {} simulations", simulationsToInclude);
      
      filledPrompt = ProfileAiOverviewUtil.fillPrompt(prompt.getPrompt(), analytics);
      log.info("[AI Overview] Filled prompt length: {} characters", filledPrompt.length());
      
      if (filledPrompt.length() > MAX_PROMPT_LENGTH) {
        if (simulationsToInclude > 1) {
          simulationsToInclude--;
          log.warn("[AI Overview] Prompt too long ({} chars), reducing to {} simulations", 
            filledPrompt.length(), simulationsToInclude);
        } else {
          log.warn("[AI Overview] Prompt still too long ({} chars) even with 1 simulation. Will truncate.", 
            filledPrompt.length());
          break;
        }
      } else {
        break;
      }
    } while (simulationsToInclude >= 1);
    
    log.debug("[AI Overview] Final filled prompt:\n{}", filledPrompt);

    String overviewText = null;
    com.fasterxml.jackson.databind.JsonNode overviewJson = null;
    boolean success = false;
    String errorMessage = null;

    try {
      overviewText = chatGptService.generateOverview(filledPrompt, prompt.getAssistantId(), LLM_MODEL);
      
      // Check if response is null or empty
      if (overviewText == null || overviewText.trim().isEmpty()) {
        success = false;
        errorMessage = "LLM returned null or empty response";
        log.warn("[AI Overview] Generation failed: {}", errorMessage);
      } else {
        log.info("[AI Overview] LLM response received (length: {} characters)", overviewText.length());
        log.debug("[AI Overview] LLM response:\n{}", overviewText);
        
        // Try to parse as JSON only if we have content
        try {
          overviewJson = new com.fasterxml.jackson.databind.ObjectMapper().readTree(overviewText);
          success = true;
          log.info("[AI Overview] Successfully parsed LLM response as JSON");
        } catch (Exception e) {
          success = false;
          errorMessage = "Failed to parse LLM response as JSON: " + e.getMessage();
          log.error("[AI Overview] {}", errorMessage, e);
          // Keep the text even if JSON parsing fails
        }
      }
      
      if (success) {
        log.info("[AI Overview] Generation successful for user: {}", user.getEmail());
      }
    } catch (Exception e) {
      success = false;
      errorMessage = "Exception during generation: " + e.getMessage();
      log.error("[AI Overview] Generation failed with exception for user {}: {}", 
        user.getEmail(), errorMessage, e);
    }

    // Always save the overview, even if generation failed
    // This allows tracking of failed attempts and prevents infinite retries
    AiOverview overview = AiOverview.builder()
      .entityType(PROFILE_ENTITY_TYPE)
      .entityId(user.getId())
      .overviewText(success ? overviewText : ("Generation failed: " + errorMessage))
      .overviewJson(overviewJson)
      .promptUsed(filledPrompt)
      .llmModel(LLM_MODEL)
      .paramsJson(ProfileAiOverviewUtil.analyticsToJson(analytics))
      .source("profile_progression")
      .createdAt(LocalDateTime.now())
      .updatedAt(LocalDateTime.now())
      .build();
    
    try {
      aiOverviewRepository.save(overview);
      log.info("[AI Overview] Overview saved for user: {} (success: {})", user.getEmail(), success);
    } catch (Exception e) {
      log.error("[AI Overview] Failed to save overview for user: {}", user.getEmail(), e);
      throw e;
    }
    
    return overview;
  }

  // Scheduled job: every 10 minutes, only for users with recent hyperparam updates
  @Scheduled(cron = "0 */10 * * * *")
  public void scheduledProfileOverviewUpdate() {
    LocalDateTime since = LocalDateTime.now().minusHours(3);
    List<User> users = userHyperParameterRepository.findUsersWithRecentHyperparamUpdates(since);
    users.forEach(user -> {
      try {
        // Check if the latest overview is older than 24 hours
        Optional<AiOverview> latestOverviewOpt = getLatestProfileOverview(user.getId());
        boolean shouldGenerate = latestOverviewOpt
          .map(overview -> overview.getCreatedAt() == null || overview.getCreatedAt()
            .isBefore(LocalDateTime.now().minusHours(24)))
          .orElse(true);

        if (shouldGenerate) {
          log.info("[AI Overview] Generating for user (no recent overview): {}", user.getEmail());
          generateAndStoreProfileOverview(user);
        } else {
          log.info("[AI Overview] Skipping user (overview is recent): {}", user.getEmail());
        }
      } catch (Exception e) {
        log.error("Failed to generate AI overview for user {}", user.getEmail(), e);
      }
    });
  }
}
