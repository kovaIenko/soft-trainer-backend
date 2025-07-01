package com.backend.softtrainer.services.analytics;

import com.backend.softtrainer.entities.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ProfileAiOverviewUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Map<String, Object> collectUserAnalytics(User user, ProfileAnalyticsService analyticsService) {
        return collectUserAnalytics(user, analyticsService, null);
    }

    public static Map<String, Object> collectUserAnalytics(User user, ProfileAnalyticsService analyticsService, Integer maxSimulations) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", user.getName());
        map.put("department", user.getDepartment());
        map.put("localization", user.getOrganization().getLocalization());

        // Use reflection for optional fields
        addIfPresent(map, user, "getPosition", "position");
        addIfPresent(map, user, "getSkills", "skills");
        addIfPresent(map, user, "getAchievements", "achievements");
        addIfPresent(map, user, "getPreferences", "preferences");
        addIfPresent(map, user, "getOtherContext", "other_context");
        // Add analytics (e.g., radar, progression)
        map.put("hyperparams", analyticsService.getProfileRadar(user.getEmail()));
        map.put("simulation_results", analyticsService.getProfileProgression(user.getEmail(), maxSimulations));
        return map;
    }

    private static void addIfPresent(Map<String, Object> map, User user, String methodName, String key) {
        try {
            Method m = user.getClass().getMethod(methodName);
            Object value = m.invoke(user);
            map.put(key, value);
        } catch (Exception ignored) {}
    }

    public static String fillPrompt(String promptPattern, Map<String, Object> analytics) {
        String prompt = promptPattern;
        for (Map.Entry<String, Object> entry : analytics.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue().toString();
            prompt = prompt.replace("{" + entry.getKey() + "}", value);
        }
        return prompt;
    }

    public static JsonNode analyticsToJson(Map<String, Object> analytics) {
        try {
            return objectMapper.valueToTree(analytics);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }
}
