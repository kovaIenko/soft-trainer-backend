package com.backend.softtrainer.services.analytics;

import com.backend.softtrainer.dtos.analytics.HyperParamRatioDto;
import com.backend.softtrainer.dtos.analytics.TeamHeatmapDto;
import com.backend.softtrainer.dtos.analytics.UserHeatmapDto;
import com.backend.softtrainer.entities.Organization;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.entities.UserHyperParameter;
import com.backend.softtrainer.repositories.OrganizationRepository;
import com.backend.softtrainer.repositories.UserRepository;
import com.backend.softtrainer.services.UserHyperParameterService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@AllArgsConstructor
public class TeamAnalyticsService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserHyperParameterService userHyperParameterService;

    public TeamHeatmapDto getTeamHeatmap(String orgName) {
        Organization org = organizationRepository.findByName(orgName).orElse(null);
        if (org == null) return new TeamHeatmapDto(Collections.emptyList(), Collections.emptyList());
        List<User> users = userRepository.findAllByOrganization(org);
        if (users.isEmpty()) return new TeamHeatmapDto(Collections.emptyList(), Collections.emptyList());

        // 1. Gather all UserHyperParameter records for all users in the org
        Map<Long, List<UserHyperParameter>> userParamsMap = new HashMap<>();
        Set<String> allKeys = new HashSet<>();
        List<UserHyperParameter> allParams = new ArrayList<>();
        for (User user : users) {
            List<UserHyperParameter> params = userHyperParameterService.findAllByUser(user);
            userParamsMap.put(user.getId(), params);
            allParams.addAll(params);
            for (UserHyperParameter param : params) {
                if (param.getKey() != null) {
                    allKeys.add(param.getKey().toLowerCase().trim());
                }
            }
        }

        // 2. Compute max for each key (column) across all users' simulations
        Map<String, Double> columnMax = new HashMap<>();
        for (String key : allKeys) {
            double max = allParams.stream()
                .filter(p -> p.getKey() != null && p.getKey().equalsIgnoreCase(key) && p.getValue() != null)
                .mapToDouble(UserHyperParameter::getValue)
                .max()
                .orElse(1.0);
            columnMax.put(key, max > 0 ? max : 1.0); // avoid zero division
        }

        // 3. For each user, compute average per hyperparam, then normalize
        List<UserHeatmapDto> userDtos = new ArrayList<>();
        for (User user : users) {
            List<UserHyperParameter> params = userParamsMap.getOrDefault(user.getId(), List.of());
            Map<String, List<Double>> keyToValues = new HashMap<>();
            for (UserHyperParameter param : params) {
                if (param.getKey() != null && param.getValue() != null) {
                    String key = param.getKey().toLowerCase().trim();
                    keyToValues.computeIfAbsent(key, k -> new ArrayList<>()).add(param.getValue());
                }
            }
            List<HyperParamRatioDto> paramDtos = new ArrayList<>();
            for (String key : allKeys) {
                List<Double> values = keyToValues.getOrDefault(key, List.of());
                if (!values.isEmpty()) {
                    double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    double max = columnMax.getOrDefault(key, 1.0);
                    double normalizedValue = (avg / max) * 100;
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
