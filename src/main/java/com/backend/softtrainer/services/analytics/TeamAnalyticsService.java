package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.analytics.HyperParamRatioDto;
import com.backend.softtrainer.dtos.analytics.TeamHeatmapDto;
import com.backend.softtrainer.dtos.analytics.UserHeatmapDto;
import com.backend.softtrainer.dtos.SumHyperParamDto;
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
        Set<String> allKeys = new HashSet<>();
        
        // 1. Collect all user hyperparams, merged by key
        List<Map<String, Double>> allUserParams = new ArrayList<>();
        for (User user : users) {
            var hyperParams = userHyperParameterService.sumUpByUser(user);
            Map<String, Double> merged = hyperParams.stream()
                .collect(Collectors.groupingBy(
                    param -> param.key().toLowerCase().trim(),
                    Collectors.summingDouble(param -> param.value())
                ));
            allUserParams.add(merged);
            allKeys.addAll(merged.keySet());
        }

        // 2. Compute max for each key (column) across all users
        Map<String, Double> columnMax = new HashMap<>();
        for (String key : allKeys) {
            double max = allUserParams.stream()
                .mapToDouble(m -> m.getOrDefault(key, 0.0))
                .max()
                .orElse(1.0);
            columnMax.put(key, max > 0 ? max : 1.0); // avoid zero division
        }

        // 3. Build userDtos with normalized values, only for non-zero values
        List<UserHeatmapDto> userDtos = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            Map<String, Double> merged = allUserParams.get(i);
            List<HyperParamRatioDto> paramDtos = new ArrayList<>();
            
            // Only process hyperparameters that have values
            for (String key : allKeys) {
                double rawValue = merged.getOrDefault(key, 0.0);
                if (rawValue > 0) {  // Only create DTO for non-zero values
                    double max = columnMax.getOrDefault(key, 1.0);
                    double normalizedValue = (rawValue / max) * 100;
                    double ratio = normalizedValue / 100.0;
                    paramDtos.add(new HyperParamRatioDto(key, normalizedValue, 100.0, ratio));
                }
            }
            userDtos.add(new UserHeatmapDto(user.getEmail(), user.getName(), paramDtos));
        }
        return new TeamHeatmapDto(userDtos, new ArrayList<>(allKeys));
    }

    public Object getTeamSummary(String orgName) {
        // For now, return the same as team heatmap (or implement your classic table logic here)
        return getTeamHeatmap(orgName);
    }
}
