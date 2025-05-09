package com.backend.softtrainer.services;

import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@AllArgsConstructor
public class DashboardAnalyticsService {

    private final TeamAnalyticsService teamAnalyticsService;
    private final ProfileAnalyticsService profileAnalyticsService;
    private final UserRepository userRepository;

    private boolean isOwner(User user) {
        return user.getRoles().stream().anyMatch(r -> r.getName().toString().equals("ROLE_OWNER"));
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> r.getName().toString().equals("ROLE_ADMIN"));
    }

    private boolean isSameOrg(User user, String orgName) {
        return user.getOrganization() != null && user.getOrganization().getName().equals(orgName);
    }

    private boolean isSameUser(User user, String userEmail) {
        return user.getEmail().equalsIgnoreCase(userEmail);
    }

    public Object getAnalytics(String type, String orgName, String userEmail, Long simulationId, User currentUser) {
        switch (type) {
            case "team_summary":
            case "team_heatmap":
                if (!isOwner(currentUser) && (orgName == null || !isSameOrg(currentUser, orgName))) {
                    throw new AccessDeniedException("You do not have permission to access this organization's data.");
                }
                return teamAnalyticsService.getTeamHeatmap(orgName);
            case "profile":
            case "profile_radar":
                if (userEmail == null) {
                    throw new AccessDeniedException("User email must be provided.");
                }
                User targetUser = userRepository.findByEmail(userEmail).orElseThrow(() -> new AccessDeniedException("User not found"));
                if (!isOwner(currentUser) &&
                    !(isAdmin(currentUser) && isSameOrg(currentUser, targetUser.getOrganization().getName())) &&
                    !isSameUser(currentUser, userEmail)) {
                    throw new AccessDeniedException("You do not have permission to access this user's data.");
                }
                return profileAnalyticsService.getProfileRadar(userEmail);
            case "hyperparam_max_values":
                // Disabled: return empty map
                return Collections.emptyMap();
            // Add more cases as needed, e.g. for simulationId
            default:
                throw new IllegalArgumentException("Unknown analytics type: " + type);
        }
    }
}
