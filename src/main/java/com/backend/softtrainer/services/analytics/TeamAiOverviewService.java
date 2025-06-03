package com.backend.softtrainer.services.analytics;

import com.backend.softtrainer.entities.AiOverview;
import com.backend.softtrainer.entities.Organization;
import com.backend.softtrainer.entities.Prompt;
import com.backend.softtrainer.entities.PromptName;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.AiOverviewRepository;
import com.backend.softtrainer.repositories.OrganizationRepository;
import com.backend.softtrainer.repositories.PromptRepository;
import com.backend.softtrainer.repositories.UserHyperParameterRepository;
import com.backend.softtrainer.repositories.UserRepository;
import com.backend.softtrainer.services.chatgpt.ChatGptService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@AllArgsConstructor
@Slf4j
public class TeamAiOverviewService {
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final UserHyperParameterRepository userHyperParameterRepository;
    private final AiOverviewRepository aiOverviewRepository;
    private final PromptRepository promptRepository;
    private final TeamAnalyticsService teamAnalyticsService;
    private final ProfileAnalyticsService profileAnalyticsService;
    private final ChatGptService chatGptService;

    private static final String TEAM_ENTITY_TYPE = "TEAM";
    private static final String LLM_MODEL = "gpt-3.5-turbo";
    private static final PromptName PROMPT_NAME = PromptName.TEAM_AI_OVERVIEW;
    private static final int MAX_SIMULATIONS_FOR_OVERVIEW = 2;
    private static final int MAX_ORGANIZATIONS_PER_BATCH = 10;
    private static final int MAX_RETRIES = 3;
    private static final int LLM_TIMEOUT_SECONDS = 30;

    public Optional<AiOverview> getLatestTeamOverview(Long organizationId) {
        return aiOverviewRepository.findLatestByEntity(TEAM_ENTITY_TYPE, organizationId);
    }

    @Transactional
    public AiOverview generateAndStoreTeamOverview(Organization organization, boolean ignoreRecentCheck) {
        // Check if a recent overview exists (within 15 days), unless ignoring
        if (!ignoreRecentCheck) {
            Optional<AiOverview> latestOverviewOpt = getLatestTeamOverview(organization.getId());
            if (latestOverviewOpt.isPresent()) {
                AiOverview latestOverview = latestOverviewOpt.get();
                if (latestOverview.getCreatedAt() != null && latestOverview.getCreatedAt().isAfter(LocalDateTime.now().minusDays(15))) {
                    log.info("[Team AI Overview] Skipping generation for organization: {} (ID: {}) - recent overview exists (created at: {})", organization.getName(), organization.getId(), latestOverview.getCreatedAt());
                    return latestOverview;
                }
            }
        }

        log.info("[Team AI Overview] Starting generation for organization: {} (ID: {})", organization.getName(), organization.getId());
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("collect-analytics");

        Prompt prompt = promptRepository.findFirstByNameOrderByIdDesc(PROMPT_NAME)
            .orElseThrow(() -> new RuntimeException("Prompt for team AI overview not found"));
        log.info("[Team AI Overview] Retrieved prompt: {} (ID: {})", PROMPT_NAME, prompt.getId());

        Map<String, Object> analytics = collectTeamAnalytics(organization);
        stopWatch.stop();
        log.info("[Team AI Overview] Analytics collection completed in {} ms", stopWatch.getLastTaskTimeMillis());

        log.info("[Team AI Overview] Analytics data size: {} entries", analytics.size());
        log.debug("[Team AI Overview] Full analytics data: {}", analytics);

        String filledPrompt = fillPrompt(prompt.getPrompt(), analytics);
        log.info("[Team AI Overview] Prompt filled with analytics data (length: {} chars)", filledPrompt.length());
        log.debug("[Team AI Overview] Filled prompt:\n{}", filledPrompt);

        String overviewText = null;
        com.fasterxml.jackson.databind.JsonNode overviewJson = null;
        boolean success = false;
        String errorMessage = null;
        int retryCount = 0;

        stopWatch.start("llm-generation");
        while (retryCount < MAX_RETRIES && !success) {
            log.info("[Team AI Overview] Attempting LLM generation (attempt {}/{})", retryCount + 1, MAX_RETRIES);
            try {
                overviewText = chatGptService.generateOverview(filledPrompt, prompt.getAssistantId(), LLM_MODEL);
                if (overviewText == null || overviewText.trim().isEmpty()) {
                    success = false;
                    errorMessage = "LLM returned empty response";
                    log.warn("[Team AI Overview] Generation failed: {}", errorMessage);
                } else {
                    try {
                        overviewJson = new com.fasterxml.jackson.databind.ObjectMapper().readTree(overviewText);
                        success = true;
                        log.info("[Team AI Overview] Successfully parsed LLM response as JSON");
                    } catch (Exception e) {
                        log.error("[Team AI Overview] Failed to parse LLM response as JSON: {}", e.getMessage(), e);
                        success = false;
                        errorMessage = "Invalid JSON response";
                    }
                }
            } catch (Exception e) {
                success = false;
                errorMessage = e.getMessage();
                log.error("[Team AI Overview] Generation failed with exception: {}", errorMessage, e);
            }

            if (!success && retryCount < MAX_RETRIES - 1) {
                retryCount++;
                log.info("[Team AI Overview] Retrying generation (attempt {}/{})", retryCount + 1, MAX_RETRIES);
                try {
                    Thread.sleep(1000 * retryCount); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        stopWatch.stop();
        log.info("[Team AI Overview] LLM generation completed in {} ms", stopWatch.getLastTaskTimeMillis());

        if (!success) {
            log.error("[Team AI Overview] Failed to generate overview after {} attempts: {}", MAX_RETRIES, errorMessage);
            return null;
        }

        AiOverview overview = AiOverview.builder()
            .entityType(TEAM_ENTITY_TYPE)
            .entityId(organization.getId())
            .overviewText(overviewText)
            .overviewJson(overviewJson)
            .promptUsed(filledPrompt)
            .llmModel(LLM_MODEL)
            .paramsJson(analyticsToJson(analytics))
            .source("team_analytics")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        stopWatch.start("save-overview");
        aiOverviewRepository.save(overview);
        stopWatch.stop();

        log.info("[Team AI Overview] Overview saved successfully (ID: {}) in {} ms",
            overview.getId(), stopWatch.getLastTaskTimeMillis());
        log.info("[Team AI Overview] Total generation time: {} ms", stopWatch.getTotalTimeMillis());
        return overview;
    }

    private Map<String, Object> collectTeamAnalytics(Organization organization) {
        log.info("[Team AI Overview] Collecting analytics for organization: {}", organization.getName());
        Map<String, Object> analytics = new HashMap<>();

        // Basic organization info
        analytics.put("organization_name", organization.getName());
        analytics.put("localization", organization.getLocalization());
        analytics.put("available_skills", organization.getAvailableSkills());
        log.debug("[Team AI Overview] Basic org info collected: name={}, localization={}, skills={}",
            organization.getName(), organization.getLocalization(), organization.getAvailableSkills());

        // Team members data
        List<User> teamMembers = userRepository.findAllByOrganization(organization);
        analytics.put("team_size", teamMembers.size());
        log.info("[Team AI Overview] Found {} team members", teamMembers.size());

        // Department distribution
        Map<String, Long> departmentDistribution = teamMembers.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                user -> user.getDepartment() != null ? user.getDepartment() : "Unassigned",
                java.util.stream.Collectors.counting()
            ));
        analytics.put("department_distribution", departmentDistribution);
        log.debug("[Team AI Overview] Department distribution: {}", departmentDistribution);

        // Team heatmap data
        var heatmap = teamAnalyticsService.getTeamHeatmap(organization.getName());
        analytics.put("team_heatmap", heatmap);
        log.debug("[Team AI Overview] Team heatmap collected: {}", heatmap);

        // Individual member analytics
        List<Map<String, Object>> memberAnalytics = teamMembers.stream()
            .map(member -> {
                log.debug("[Team AI Overview] Collecting analytics for member: {}", member.getEmail());
                Map<String, Object> memberData = new HashMap<>();
                memberData.put("name", member.getName());
                memberData.put("email", member.getEmail());
                memberData.put("username", member.getUsername());
                memberData.put("department", member.getDepartment());
                memberData.put("roles", member.getRoles());
                memberData.put("hyperparams", profileAnalyticsService.getProfileRadar(member.getEmail()));
                memberData.put("progression", profileAnalyticsService.getProfileProgression(member.getEmail(), MAX_SIMULATIONS_FOR_OVERVIEW));
                return memberData;
            })
            .toList();
        analytics.put("member_analytics", memberAnalytics);
        log.info("[Team AI Overview] Collected analytics for {} team members", memberAnalytics.size());

        return analytics;
    }

    private String fillPrompt(String promptPattern, Map<String, Object> analytics) {
        String prompt = promptPattern;
        for (Map.Entry<String, Object> entry : analytics.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue().toString();
            prompt = prompt.replace("{" + entry.getKey() + "}", value);
        }
        return prompt;
    }

    private com.fasterxml.jackson.databind.JsonNode analyticsToJson(Map<String, Object> analytics) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.valueToTree(analytics);
        } catch (Exception e) {
            log.error("[Team AI Overview] Failed to convert analytics to JSON: {}", e.getMessage(), e);
            return new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        }
    }

    @Scheduled(cron = "0 0 12 * * *")
    public void scheduledTeamOverviewUpdate() {
        log.info("Starting scheduled team overview update at {}", LocalDateTime.now());
        final var MIN_ACTIVE_USERS = 2;
        final var HOURS_LOOKBACK = 24;

        var organizations = organizationRepository.findAll();
        var cutoffTime = LocalDateTime.now().minusHours(HOURS_LOOKBACK);

        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        log.info("Found {} organizations for team overview update", organizations.size());

        organizations
            .forEach(org -> {
                try {
                    var orgUsers = userRepository.findAllByOrganization(org);

                    // Count users with recent hyperparameter updates
                    var activeUsers = orgUsers.stream()
                        .filter(user -> userHyperParameterRepository.hasRecentUpdates(user.getId(), cutoffTime))
                        .count();

                    // Check if a recent overview exists (within 15 days)
                    Optional<AiOverview> latestOverviewOpt = getLatestTeamOverview(org.getId());
                    boolean hasRecentOverview = latestOverviewOpt.isPresent() &&
                        latestOverviewOpt.get().getCreatedAt() != null &&
                        latestOverviewOpt.get().getCreatedAt().isAfter(LocalDateTime.now().minusDays(15));

                    if (activeUsers >= MIN_ACTIVE_USERS && !hasRecentOverview) {
                        log.info("Processing team overview for organization: {} ({} active users)", org.getName(), activeUsers);
                        var overview = generateAndStoreTeamOverview(org, false);
                        if (overview != null) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } else if (hasRecentOverview) {
                        log.info("Skipping organization {} - recent overview exists (created at: {})", org.getName(), latestOverviewOpt.get().getCreatedAt());
                    } else {
                        log.debug("Skipping organization {} - only {} active users (minimum required: {})",
                            org.getName(), activeUsers, MIN_ACTIVE_USERS);
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    log.error("Error processing team overview for organization {}: {}", org.getName(), e.getMessage(), e);
                } finally {
                    processedCount.incrementAndGet();
                }
            });

        log.info("Completed scheduled team overview update at {}. Processed: {}, Success: {}, Errors: {}",
            LocalDateTime.now(), processedCount.get(), successCount.get(), errorCount.get());
    }
}
