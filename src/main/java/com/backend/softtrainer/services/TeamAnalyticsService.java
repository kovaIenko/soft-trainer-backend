package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.analytics.HyperParamRatioDto;
import com.backend.softtrainer.dtos.analytics.TeamHeatmapDto;
import com.backend.softtrainer.dtos.analytics.UserHeatmapDto;
import com.backend.softtrainer.entities.Organization;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.OrganizationRepository;
import com.backend.softtrainer.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TeamAnalyticsService {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserHyperParameterService userHyperParameterService;

    // This should be replaced with a real max value provider
    private Map<String, Double> getMaxValuesForOrg(String orgName) {
        // TODO: Replace with real logic (e.g., from DB or config)
        Map<String, Double> maxValues = new HashMap<>();
        maxValues.put("clarity politeness", 75.0);
        maxValues.put("problem solving", 67.0);
        maxValues.put("tension reduction", 67.0);
        maxValues.put("support trust", 67.0);
        // ... add more as needed
        return maxValues;
    }

    public TeamHeatmapDto getTeamHeatmap(String orgName) {
        Organization org = organizationRepository.findByName(orgName).orElse(null);
        if (org == null) return new TeamHeatmapDto(Collections.emptyList(), Collections.emptyList());
        List<User> users = userRepository.findAllByOrganization(org);
        Map<String, Double> maxValues = getMaxValuesForOrg(orgName);
        Set<String> allKeys = new HashSet<>(maxValues.keySet());
        List<UserHeatmapDto> userDtos = new ArrayList<>();
        for (User user : users) {
            var hyperParams = userHyperParameterService.sumUpByUser(user);
            // Merge by lowercase key and sum values
            Map<String, Double> merged = hyperParams.stream()
                .collect(Collectors.groupingBy(
                    param -> param.key().toLowerCase().trim(),
                    Collectors.summingDouble(param -> param.value())
                ));
            List<HyperParamRatioDto> paramDtos = merged.entrySet().stream().map(entry -> {
                String key = entry.getKey();
                double value = entry.getValue();
                double max = maxValues.getOrDefault(key, 1.0);
                double ratio = max > 0 ? value / max : 0.0;
                allKeys.add(key);
                return new HyperParamRatioDto(key, value, max, ratio);
            }).collect(Collectors.toList());
            userDtos.add(new UserHeatmapDto(user.getEmail(), user.getName(), paramDtos));
        }
        return new TeamHeatmapDto(userDtos, new ArrayList<>(allKeys));
    }

    public Object getTeamSummary(String orgName) {
        // For now, return the same as team heatmap (or implement your classic table logic here)
        return getTeamHeatmap(orgName);
    }
}
