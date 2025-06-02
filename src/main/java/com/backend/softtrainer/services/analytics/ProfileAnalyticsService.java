package com.backend.softtrainer.services.analytics;

import com.backend.softtrainer.dtos.analytics.HyperParamRatioDto;
import com.backend.softtrainer.dtos.analytics.ProfileRadarDto;
import com.backend.softtrainer.dtos.analytics.ProfileProgressionDto;
import com.backend.softtrainer.dtos.analytics.HyperParamProgressionDto;
import com.backend.softtrainer.dtos.analytics.SkillProgressionDto;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.entities.UserHyperParameter;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.events.HyperParameterUpdatedEvent;
import com.backend.softtrainer.repositories.UserHyperParameterRepository;
import com.backend.softtrainer.repositories.UserRepository;
import com.backend.softtrainer.repositories.ChatRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ProfileAnalyticsService {
    private final UserRepository userRepository;
  private final UserHyperParameterRepository userHyperParameterRepository;
    private final ChatRepository chatRepository;

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

    // Cache max values for 1 hour
    @Cacheable(value = "maxHyperParamValues", key = "'all'")
    public Map<String, Double> getMaxHyperParamValues() {
        List<UserHyperParameter> allParams = userHyperParameterRepository.findAll();
        return allParams.stream()
            .filter(param -> param.getKey() != null && param.getValue() != null)
            .collect(Collectors.groupingBy(
                param -> param.getKey().toLowerCase().trim(),
                Collectors.collectingAndThen(
                    Collectors.mapping(
                        UserHyperParameter::getValue,
                        Collectors.maxBy(Double::compareTo)
                    ),
                    opt -> opt.orElse(1.0)
                )
            ));
    }

    // Evict max values cache every hour
    @Scheduled(fixedRate = 3600000)
    @CacheEvict(value = "maxHyperParamValues", allEntries = true)
    public void evictMaxValuesCache() {
        // Method to evict cache
    }

    // Cache profile progression for 5 minutes
    @Cacheable(value = "profileProgression", key = "#userEmail + '-' + #maxSimulations")
    public ProfileProgressionDto getProfileProgression(String userEmail, Integer maxSimulations) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) return null;

        // 1. Get all user hyperparameters with a single query
        List<UserHyperParameter> allUserParams = userHyperParameterRepository.findAllByOwnerId(user.getId());
        if (allUserParams.isEmpty()) return null;

        // 2. Get all relevant chats in a single query
        Set<Long> chatIds = allUserParams.stream()
            .map(UserHyperParameter::getChatId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, LocalDateTime> chatTimestamps = chatRepository.findAllById(chatIds).stream()
            .collect(Collectors.toMap(
                Chat::getId,
                Chat::getTimestamp
            ));

        // 3. Calculate max value for each (simulation, hyperparam) pair across all users
        Map<String, Double> simHyperparamMax = new HashMap<>();
        List<UserHyperParameter> allParams = userHyperParameterRepository.findAll();
        for (UserHyperParameter param : allParams) {
            if (param.getKey() == null || param.getSimulationId() == null || param.getValue() == null) continue;
            String simKey = param.getSimulationId() + "::" + param.getKey().toLowerCase().trim();
            double value = param.getValue();
            simHyperparamMax.merge(simKey, value, Math::max);
        }

        // 4. Group by hyperparam key (merge all simulation data for the same hyperparam)
        Map<String, List<UserHyperParameter>> hyperparamGroups = new HashMap<>();
        for (UserHyperParameter param : allUserParams) {
            if (param.getKey() == null) continue;
            String key = param.getKey().toLowerCase().trim();
            hyperparamGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(param);
        }

        // 5. Build progression data efficiently
        List<HyperParamProgressionDto> progressionData = new ArrayList<>();
        for (Map.Entry<String, List<UserHyperParameter>> entry : hyperparamGroups.entrySet()) {
            String paramKey = entry.getKey();
            List<UserHyperParameter> paramValues = entry.getValue();

            // Sort by chat timestamp
            paramValues.sort(Comparator.comparing(param ->
                chatTimestamps.getOrDefault(param.getChatId(), LocalDateTime.MIN)
            ));

            // If maxSimulations is provided, limit the number of simulations
            if (maxSimulations != null && maxSimulations > 0) {
                // Get unique simulation IDs
                Set<Long> uniqueSimIds = paramValues.stream()
                    .map(UserHyperParameter::getSimulationId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

                // If we have more simulations than the limit, keep only the most recent ones
                if (uniqueSimIds.size() > maxSimulations) {
                    // Get the most recent simulation IDs
                    Set<Long> recentSimIds = paramValues.stream()
                        .sorted(Comparator.comparing((UserHyperParameter param) ->
                            chatTimestamps.getOrDefault(param.getChatId(), LocalDateTime.MIN)
                        ).reversed())
                        .map(UserHyperParameter::getSimulationId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .limit(maxSimulations)
                        .collect(Collectors.toSet());

                    // Filter paramValues to only include the most recent simulations
                    paramValues = paramValues.stream()
                        .filter(param -> recentSimIds.contains(param.getSimulationId()))
                        .collect(Collectors.toList());
                }
            }

            // Calculate normalized scores per simulation
            List<SkillProgressionDto> scores = paramValues.stream()
                .map(param -> {
                    String simKey = (param.getSimulationId() != null ? param.getSimulationId() : 0L) + "::" + paramKey;
                    double maxValue = Math.max(1.0, simHyperparamMax.getOrDefault(simKey, 1.0));
                    double rawValue = param.getValue() == null ? 0.0 : Math.max(0, param.getValue());
                    double normalizedScore = (rawValue / maxValue) * 100;
                    if (Double.isNaN(normalizedScore) || Double.isInfinite(normalizedScore)) {
                        normalizedScore = 0.0;
                    }
                    LocalDateTime timestamp = chatTimestamps.getOrDefault(param.getChatId(), LocalDateTime.MIN);
                    return new SkillProgressionDto(
                        timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
                        normalizedScore
                    );
                })
                .collect(Collectors.toList());

            if (!scores.isEmpty()) {
                progressionData.add(new HyperParamProgressionDto(paramKey, scores));
            }
        }

        return new ProfileProgressionDto(
            user.getId().toString(),
            user.getName(),
            progressionData
        );
    }

    // Add overloaded method for backward compatibility
    @Cacheable(value = "profileProgression", key = "#userEmail")
    public ProfileProgressionDto getProfileProgression(String userEmail) {
        return getProfileProgression(userEmail, null);
    }

    // Evict profile progression cache when new data is added
    @CacheEvict(value = "profileProgression", key = "#userEmail")
    public void evictProfileProgressionCache(String userEmail) {
        // Method to evict cache
    }

    @EventListener
    public void handleHyperParameterUpdated(HyperParameterUpdatedEvent event) {
        log.info("Received hyperparameter update event for user: {}", event.getUserEmail());
        evictProfileProgressionCache(event.getUserEmail());
    }
}
