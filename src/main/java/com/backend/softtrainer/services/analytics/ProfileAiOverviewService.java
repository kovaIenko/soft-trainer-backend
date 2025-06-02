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
  private static final int MAX_SIMULATIONS_FOR_OVERVIEW = 5; // Limit to last 5 simulations for AI overview

  public Optional<AiOverview> getLatestProfileOverview(Long userId) {
    return aiOverviewRepository.findLatestByEntity(PROFILE_ENTITY_TYPE, userId);
  }

  public AiOverview generateAndStoreProfileOverview(User user) {
    Prompt prompt = promptRepository.findFirstByNameOrderByIdDesc(PROMPT_NAME)
      .orElseThrow(() -> new RuntimeException("Prompt for profile AI overview not found"));

    Map<String, Object> analytics = ProfileAiOverviewUtil.collectUserAnalytics(user, profileAnalyticsService, MAX_SIMULATIONS_FOR_OVERVIEW);

    log.info("[AI Overview] Prompt template:\n{}", prompt.getPrompt());
    log.info("[AI Overview] Analytics/user data: {}", analytics);
    String filledPrompt = ProfileAiOverviewUtil.fillPrompt(prompt.getPrompt(), analytics);
    log.info("[AI Overview] Filled prompt:\n{}", filledPrompt);

    String overviewText = null;
    com.fasterxml.jackson.databind.JsonNode overviewJson = null;
    boolean success = true;
    String errorMessage = null;

    try {
      overviewText = chatGptService.generateOverview(filledPrompt, prompt.getAssistantId(), LLM_MODEL);
      log.info("[AI Overview] LLM response:\n{}", overviewText);
      if (overviewText == null || overviewText.trim().isEmpty()) {
        success = false;
        errorMessage = "LLM returned empty response";
        log.warn("[AI Overview] Generation failed: {}", errorMessage);
      } else {
        // Try to parse as JSON
        try {
          overviewJson = new com.fasterxml.jackson.databind.ObjectMapper().readTree(overviewText);
        } catch (Exception e) {
          log.error("[AI Overview] Failed to parse LLM response as JSON", e);
        }
        log.info("[AI Overview] Generation successful for user: {}", user.getEmail());
      }
    } catch (Exception e) {
      success = false;
      errorMessage = e.getMessage();
      log.error("[AI Overview] Generation failed with exception: {}", errorMessage, e);
    }

    AiOverview overview = AiOverview.builder()
      .entityType(PROFILE_ENTITY_TYPE)
      .entityId(user.getId())
      .overviewText(overviewText)
      .overviewJson(overviewJson)
      .promptUsed(filledPrompt)
      .llmModel(LLM_MODEL)
      .paramsJson(ProfileAiOverviewUtil.analyticsToJson(analytics))
      .source("profile_progression")
      .createdAt(LocalDateTime.now())
      .updatedAt(LocalDateTime.now())
      .build();
    aiOverviewRepository.save(overview);
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
