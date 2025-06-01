package com.backend.softtrainer.services.analytics;

import com.backend.softtrainer.entities.AiOverview;
import com.backend.softtrainer.entities.Prompt;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.entities.PromptName;
import com.backend.softtrainer.repositories.AiOverviewRepository;
import com.backend.softtrainer.repositories.PromptRepository;
import com.backend.softtrainer.repositories.UserRepository;
import com.backend.softtrainer.services.chatgpt.ChatGptService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class ProfileAiOverviewService {
    private final UserRepository userRepository;
    private final AiOverviewRepository aiOverviewRepository;
    private final PromptRepository promptRepository;
    private final ChatGptService chatGptService;
    private final ProfileAnalyticsService profileAnalyticsService;

    private static final String PROFILE_ENTITY_TYPE = "PROFILE";
    private static final String LLM_MODEL = "gpt-3.5-turbo";
    private static final PromptName PROMPT_NAME = PromptName.PROFILE_AI_OVERVIEW;

    public Optional<AiOverview> getLatestProfileOverview(Long userId) {
        return aiOverviewRepository.findLatestByEntity(PROFILE_ENTITY_TYPE, userId);
    }

    public AiOverview generateAndStoreProfileOverview(User user) {
        // 1. Fetch prompt pattern
        Prompt prompt = promptRepository.findFirstByNameOrderByIdDesc(PROMPT_NAME)
                .orElseThrow(() -> new RuntimeException("Prompt for profile AI overview not found"));

        // 2. Gather user analytics and info
        Map<String, Object> analytics = ProfileAiOverviewUtil.collectUserAnalytics(user, profileAnalyticsService);

        // 3. Fill prompt
        String filledPrompt = ProfileAiOverviewUtil.fillPrompt(prompt.getPrompt(), analytics);

        // 4. Call LLM
        String overviewText = chatGptService.generateOverview(filledPrompt, LLM_MODEL);

        // 5. Store in ai_overview (no promptId, only promptUsed)
        AiOverview overview = AiOverview.builder()
                .entityType(PROFILE_ENTITY_TYPE)
                .entityId(user.getId())
                .overviewText(overviewText)
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

    // Scheduled job: every 3 hours, update for users with recent activity
    @Scheduled(cron = "0 0 */3 * * *")
    public void scheduledProfileOverviewUpdate() {
        // TODO: Find users with recent simulation/hyperparam updates
        // For demo, update all users
        userRepository.findAll().forEach(user -> {
            try {
                generateAndStoreProfileOverview(user);
            } catch (Exception e) {
                log.error("Failed to generate AI overview for user {}", user.getEmail(), e);
            }
        });
    }
} 