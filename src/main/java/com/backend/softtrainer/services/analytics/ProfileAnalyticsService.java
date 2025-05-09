package com.backend.softtrainer.services.analytics;

import com.backend.softtrainer.dtos.analytics.HyperParamRatioDto;
import com.backend.softtrainer.dtos.analytics.ProfileRadarDto;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.entities.UserHyperParameter;
import com.backend.softtrainer.repositories.UserHyperParameterRepository;
import com.backend.softtrainer.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProfileAnalyticsService {
    private final UserRepository userRepository;
  private final UserHyperParameterRepository userHyperParameterRepository;

    public ProfileRadarDto getProfileRadar(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) return null;
        Long userId = user.getId();

        // 1. Fetch all user hyperparams to get simulation IDs
        List<UserHyperParameter> allUserParams = userHyperParameterRepository.findAll();
        List<Long> simulationIds = allUserParams.stream()
          .filter(param -> userId.equals(param.getOwnerId()))
          .map(UserHyperParameter::getSimulationId)
          .distinct()
          .toList();

        // 2. For each simulation, fetch all user hyperparams
        Map<String, List<Double>> paramValues = new HashMap<>();
        for (Long simId : simulationIds) {
            List<UserHyperParameter> params = userHyperParameterRepository.findAllByOwnerIdAndSimulationId(userId, simId);
            for (UserHyperParameter param : params) {
                String key = param.getKey().toLowerCase().trim();
                paramValues.computeIfAbsent(key, k -> new ArrayList<>()).add(param.getValue());
            }
        }

        // 3. For each hyperparam key, compute the average
        Map<String, Double> avgValues = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : paramValues.entrySet()) {
            List<Double> values = entry.getValue();
            double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            avgValues.put(entry.getKey(), avg);
        }

        // 4. Fetch max value for each hyperparam across all users (for normalization)
        Map<String, Double> maxValues = new HashMap<>();
        for (String key : avgValues.keySet()) {
            double max = allUserParams.stream()
              .filter(param -> param.getKey().equalsIgnoreCase(key))
              .mapToDouble(UserHyperParameter::getValue)
              .max()
              .orElse(1.0);
            maxValues.put(key, max > 0 ? max : 1.0);
        }

        // 5. Normalize the user's average values using these max values
        List<HyperParamRatioDto> paramDtos = avgValues.entrySet().stream()
          .filter(entry -> entry.getValue() > 0)
          .map(entry -> {
              String key = entry.getKey();
              double avg = entry.getValue();
              double max = maxValues.getOrDefault(key, 1.0);
              double normalizedValue = (avg / max) * 100;
              double ratio = normalizedValue / 100.0;
              return new HyperParamRatioDto(key, normalizedValue, 100.0, ratio);
          })
          .collect(Collectors.toList());

        return new ProfileRadarDto(user.getEmail(), user.getName(), paramDtos);
    }
}
