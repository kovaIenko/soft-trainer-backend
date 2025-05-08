package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.analytics.HyperParamRatioDto;
import com.backend.softtrainer.dtos.analytics.ProfileRadarDto;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProfileAnalyticsService {
    private final UserRepository userRepository;
    private final UserHyperParameterService userHyperParameterService;

    // This should be replaced with a real max value provider
    private Map<String, Double> getMaxValues() {
        Map<String, Double> maxValues = new HashMap<>();
        maxValues.put("clarity politeness", 75.0);
        maxValues.put("problem solving", 67.0);
        maxValues.put("tension reduction", 67.0);
        maxValues.put("support trust", 67.0);
        // ... add more as needed
        return maxValues;
    }

    public ProfileRadarDto getProfileRadar(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) return null;
        Map<String, Double> maxValues = getMaxValues();
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
            return new HyperParamRatioDto(key, value, max, ratio);
        }).collect(Collectors.toList());
        return new ProfileRadarDto(user.getEmail(), user.getName(), paramDtos);
    }
}
