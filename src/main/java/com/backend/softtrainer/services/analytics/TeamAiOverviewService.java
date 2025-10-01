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
    private static final int MAX_TEAM_MEMBERS_TO_INCLUDE = 20; // Limit members to prevent prompt overflow
    private static final int MAX_ORGANIZATIONS_PER_BATCH = 10;
    private static final int MAX_RETRIES = 3;
    private static final int LLM_TIMEOUT_SECONDS = 30;
    private static final int MAX_PROMPT_LENGTH = 255000; // OpenAI limit is 256k, keep buffer

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

        // Start with maximum limits and progressively reduce if prompt is too long
        int maxMembers = MAX_TEAM_MEMBERS_TO_INCLUDE;
        int simulationsPerMember = MAX_SIMULATIONS_FOR_OVERVIEW;
        String filledPrompt;
        Map<String, Object> analytics;
        
        // Iteratively reduce data until prompt fits
        do {
            analytics = collectTeamAnalytics(organization, maxMembers, simulationsPerMember);
            stopWatch.stop();
            log.info("[Team AI Overview] Analytics collected (members: {}, simulations: {})", 
                maxMembers, simulationsPerMember);

            filledPrompt = fillPrompt(prompt.getPrompt(), analytics);
            log.info("[Team AI Overview] Prompt filled with analytics data (length: {} chars)", filledPrompt.length());
            
            if (filledPrompt.length() > MAX_PROMPT_LENGTH) {
                // First try reducing simulations per member
                if (simulationsPerMember > 1) {
                    simulationsPerMember--;
                    log.warn("[Team AI Overview] Prompt too long ({} chars), reducing to {} simulations per member", 
                        filledPrompt.length(), simulationsPerMember);
                    stopWatch.start("collect-analytics-retry");
                // Then try reducing number of members
                } else if (maxMembers > 5) {
                    maxMembers = Math.max(5, maxMembers - 5);
                    simulationsPerMember = MAX_SIMULATIONS_FOR_OVERVIEW; // Reset simulations
                    log.warn("[Team AI Overview] Prompt too long ({} chars), reducing to {} team members", 
                        filledPrompt.length(), maxMembers);
                    stopWatch.start("collect-analytics-retry");
                } else {
                    log.warn("[Team AI Overview] Prompt still too long ({} chars) even with minimal data. Will truncate.", 
                        filledPrompt.length());
                    break;
                }
            } else {
                break;
            }
        } while (true);
        
        log.debug("[Team AI Overview] Final filled prompt:\n{}", filledPrompt);

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
                
                // Check if response is null or empty
                if (overviewText == null || overviewText.trim().isEmpty()) {
                    success = false;
                    errorMessage = "LLM returned null or empty response";
                    log.warn("[Team AI Overview] Generation failed: {}", errorMessage);
                } else {
                    log.info("[Team AI Overview] LLM response received (length: {} characters)", overviewText.length());
                    log.debug("[Team AI Overview] LLM response:\n{}", overviewText);
                    
                    // Try to parse as JSON only if we have content
                    try {
                        overviewJson = new com.fasterxml.jackson.databind.ObjectMapper().readTree(overviewText);
                        success = true;
                        log.info("[Team AI Overview] Successfully parsed LLM response as JSON");
                    } catch (Exception e) {
                        log.error("[Team AI Overview] Failed to parse LLM response as JSON: {}", e.getMessage(), e);
                        success = false;
                        errorMessage = "Invalid JSON response: " + e.getMessage();
                        // Keep the text even if JSON parsing fails
                    }
                }
            } catch (Exception e) {
                success = false;
                errorMessage = "Exception during generation: " + e.getMessage();
                log.error("[Team AI Overview] Generation failed with exception: {}", errorMessage, e);
            }

            // Retry logic with exponential backoff
            if (!success && retryCount < MAX_RETRIES - 1) {
                retryCount++;
                log.info("[Team AI Overview] Retrying generation (attempt {}/{}) after error: {}", 
                    retryCount + 1, MAX_RETRIES, errorMessage);
                try {
                    Thread.sleep(1000 * retryCount); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("[Team AI Overview] Retry interrupted");
                    break;
                }
            } else {
                break; // Exit loop if successful or no more retries
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

    private Map<String, Object> collectTeamAnalytics(Organization organization, int maxMembers, int simulationsPerMember) {
        log.info("[Team AI Overview] Collecting analytics for organization: {} (max members: {}, simulations: {})", 
            organization.getName(), maxMembers, simulationsPerMember);
        Map<String, Object> analytics = new HashMap<>();

        // Basic organization info
        analytics.put("organization_name", organization.getName());
        analytics.put("localization", organization.getLocalization());
        analytics.put("available_skills", organization.getAvailableSkills());
        log.debug("[Team AI Overview] Basic org info collected: name={}, localization={}, skills={}",
            organization.getName(), organization.getLocalization(), organization.getAvailableSkills());

        // Team members data
        List<User> allTeamMembers = userRepository.findAllByOrganization(organization);
        analytics.put("team_size", allTeamMembers.size());
        log.info("[Team AI Overview] Found {} total team members", allTeamMembers.size());

        // Limit team members - prioritize most recently active users (within last 24h)
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<User> recentlyActiveMembers = allTeamMembers.stream()
            .filter(user -> userHyperParameterRepository.hasRecentUpdates(user.getId(), cutoffTime))
            .limit(maxMembers)
            .toList();
        
        // If we don't have enough recently active members, fill with others
        List<User> teamMembers = recentlyActiveMembers;
        if (recentlyActiveMembers.size() < maxMembers && recentlyActiveMembers.size() < allTeamMembers.size()) {
            int remainingSlots = maxMembers - recentlyActiveMembers.size();
            List<User> additionalMembers = allTeamMembers.stream()
                .filter(user -> !recentlyActiveMembers.contains(user))
                .limit(remainingSlots)
                .toList();
            
            // Combine both lists
            teamMembers = new java.util.ArrayList<>(recentlyActiveMembers);
            teamMembers.addAll(additionalMembers);
        }
        
        if (allTeamMembers.size() > maxMembers) {
            log.info("[Team AI Overview] Limited to {} most active members (from {})", 
                teamMembers.size(), allTeamMembers.size());
        }

        // Department distribution (from all members)
        Map<String, Long> departmentDistribution = allTeamMembers.stream()
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

        // Individual member analytics (limited set)
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
                memberData.put("progression", profileAnalyticsService.getProfileProgression(member.getEmail(), simulationsPerMember));
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
                        latestOverviewOpt.get().getCreatedAt().isAfter(LocalDateTime.now().minusDays(1));

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
