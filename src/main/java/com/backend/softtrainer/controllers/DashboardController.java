package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.AvailableSimulationFlowDto;
import com.backend.softtrainer.dtos.SimulationNodesDto;
import com.backend.softtrainer.dtos.analytics.DashboardAnalyticsResponseDto;
import com.backend.softtrainer.dtos.analytics.UserHyperParamDto;
import com.backend.softtrainer.dtos.analytics.UsersWithHyperParamsDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.repositories.SimulationRepository;
import com.backend.softtrainer.repositories.UserRepository;
import com.backend.softtrainer.services.UserHyperParameterService;
import com.backend.softtrainer.services.auth.CustomUsrDetails;
import com.backend.softtrainer.services.auth.CustomUsrDetailsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dashboard")
@AllArgsConstructor
@Slf4j
public class DashboardController {

    private final SimulationRepository simulationRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final UserHyperParameterService userHyperParameterService;
    private final CustomUsrDetailsService customUsrDetailsService;

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
    @GetMapping("/get")
    public ResponseEntity<AvailableSimulationFlowDto> getAllUsersOfCurrentOrgWithHyperParams() {
        try {
            var openSimulations = simulationRepository.findAllSimulationsWithFlowNodes();

            //todo there is a better way to manage it while querying
            openSimulations.forEach(simulation -> {
                simulation.getNodes().forEach(node -> node.setSimulation(null));
            });

            var response = openSimulations.stream()
                .map(simulation -> new SimulationNodesDto(
                    simulation.getId(),
                    simulation.getName(),
                    simulation.getNodes(),
                    simulation.getAvatar(),
                    simulation.getComplexity(),
                    simulation.getCreatedAt().toString(),
                    null,
                    simulation.isOpen()
                ))
                .toList();

            return ResponseEntity.ok(new AvailableSimulationFlowDto(response, true, "success"));
        } catch (Exception e) {
            return ResponseEntity.ok(new AvailableSimulationFlowDto(null, false, e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
    @GetMapping("/analytics")
    public ResponseEntity<DashboardAnalyticsResponseDto> getAnalytics(Authentication authentication) {
        try {
            var userDetails = (CustomUsrDetails) customUsrDetailsService.loadUserByUsername(authentication.getName());
            var currentUser = userDetails.user();
            var organization = currentUser.getOrganization();

            var users = userRepository.findAllByOrganization(organization);
            var userAnalytics = new ArrayList<UsersWithHyperParamsDto>();

            for (User user : users) {
                var chats = chatRepository.findAllByUser(user);
                var completedSimulations = chats.stream()
                    .filter(Chat::isFinished)
                    .map(Chat::getSimulation)
                    .distinct()
                    .count();

                var totalAttempts = chats.size();
                var averageHeartsLost = chats.stream()
                    .mapToDouble(chat -> {
                        var simulation = chat.getSimulation();
                        if (simulation == null || simulation.getHearts() == null) {
                            return 0.0;
                        }
                        return simulation.getHearts() - (chat.getHearts() != null ? chat.getHearts() : 0.0);
                    })
                    .average()
                    .orElse(0.0);

                var hyperParams = userHyperParameterService.sumUpByUser(user).stream()
                    .map(param -> new UserHyperParamDto(param.key(), param.value(), null))
                    .collect(Collectors.toList());

                var totalScore = hyperParams.stream()
                    .mapToDouble(UserHyperParamDto::getValue)
                    .sum();

                userAnalytics.add(new UsersWithHyperParamsDto(
                    user.getEmail(),
                    user.getDepartment(),
                    user.getName(),
                    user.getAvatar(),
                    hyperParams,
                    totalScore,
                    (int) completedSimulations,
                    totalAttempts,
                    averageHeartsLost
                ));
            }

            return ResponseEntity.ok(new DashboardAnalyticsResponseDto(userAnalytics, true, "success"));
        } catch (Exception e) {
            log.error("Error while getting analytics for user {}", authentication.getName(), e);
            return ResponseEntity.ok(new DashboardAnalyticsResponseDto(new ArrayList<>(), false, e.getMessage()));
        }
    }
}
