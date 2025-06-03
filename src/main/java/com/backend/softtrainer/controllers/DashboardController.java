package com.backend.softtrainer.controllers;

import com.backend.softtrainer.entities.Organization;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.OrganizationRepository;
import com.backend.softtrainer.repositories.UserHyperParameterRepository;
import com.backend.softtrainer.repositories.UserRepository;
import com.backend.softtrainer.services.analytics.DashboardAnalyticsService;
import com.backend.softtrainer.services.analytics.ProfileAiOverviewService;
import com.backend.softtrainer.services.analytics.TeamAiOverviewService;
import com.backend.softtrainer.services.auth.AuthUtils;
import com.backend.softtrainer.services.auth.CustomUsrDetails;
import com.backend.softtrainer.services.auth.CustomUsrDetailsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@AllArgsConstructor
@Slf4j
public class DashboardController {

  private final CustomUsrDetailsService usrDetailsService;
  private final DashboardAnalyticsService dashboardAnalyticsService;
  private final ProfileAiOverviewService profileAiOverviewService;
  private final TeamAiOverviewService teamAiOverviewService;
  private final UserRepository userRepository;
  private final UserHyperParameterRepository userHyperParameterRepository;
  private final OrganizationRepository organizationRepository;

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
  @GetMapping("/analytics")
  public ResponseEntity<Map<String, Object>> getAnalytics(
    @RequestParam String type,
    @RequestParam(required = false) String orgName,
    @RequestParam(required = false) String userEmail,
    @RequestParam(required = false) Long simulationId,
    Authentication authentication
  ) {
    var userDetails = (CustomUsrDetails) usrDetailsService.loadUserByUsername(authentication.getName());

    try {
      if ("profile_ai_overview".equals(type)) {
        log.info("[AI Overview] Requested for userEmail: {}", userEmail);
        Map<String, Object> response = new HashMap<>();
        response.put("type", type);
        if (userEmail == null) {
          response.put("success", false);
          response.put("data", null);
          response.put("error_message", "userEmail is required");
          return ResponseEntity.badRequest().body(response);
        }
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
          response.put("success", false);
          response.put("data", null);
          response.put("error_message", "User not found");
          return ResponseEntity.badRequest().body(response);
        }
        var overview = profileAiOverviewService.getLatestProfileOverview(user.getId())
          .orElse(null);
        if (overview == null) {
          log.info("[AI Overview] No overview found for user: {}", userEmail);
          response.put("success", false);
          response.put("data", null);
          response.put("error_message", "No AI overview found for this user");
        } else {
          log.info("[AI Overview] Overview found for user: {}", userEmail);
          response.put("success", true);
          if (overview.getOverviewJson() != null) {
            response.put("data", overview.getOverviewJson());
          } else {
            response.put("data", overview.getOverviewText());
          }
          response.put("error_message", null);
        }
        return ResponseEntity.ok(response);
      } else if ("team_ai_overview".equals(type)) {
        log.info("[Team AI Overview] Requested for orgName: {}", orgName);
        Map<String, Object> response = new HashMap<>();
        response.put("type", type);
        if (orgName == null) {
          response.put("success", false);
          response.put("data", null);
          response.put("error_message", "orgName is required");
          return ResponseEntity.badRequest().body(response);
        }

        // Check if user is an owner using AuthUtils
        boolean isOwner = AuthUtils.userIsOwnerApp(authentication);
        log.info("[Team AI Overview] User {} is owner: {}", userDetails.user().getEmail(), isOwner);

        Organization targetOrg;
        if (isOwner) {
            // Owners can view any organization
            targetOrg = organizationRepository.findByName(orgName)
                .orElse(null);
            if (targetOrg != null) {
                log.info("[Team AI Overview] Owner {} accessing overview for organization: {} (id: {})",
                    userDetails.user().getEmail(), orgName, targetOrg.getId());
            } else {
                log.warn("[Team AI Overview] Owner {} attempted to access non-existent organization: {}",
                    userDetails.user().getEmail(), orgName);
            }
        } else {
            // Non-owners can only view their own organization
            targetOrg = userDetails.user().getOrganization();
            if (targetOrg == null || !targetOrg.getName().equals(orgName)) {
                log.info("[Team AI Overview] Access denied for user {} to organization {} (user's org: {})",
                    userDetails.user().getEmail(), orgName,
                    targetOrg != null ? targetOrg.getName() : "none");
                response.put("success", false);
                response.put("data", null);
                response.put("error_message", "Organization not found or access denied");
                return ResponseEntity.badRequest().body(response);
            }
            log.info("[Team AI Overview] User {} accessing their own organization: {}",
                userDetails.user().getEmail(), orgName);
        }

        if (targetOrg == null) {
            log.info("[Team AI Overview] Organization not found: {}", orgName);
            response.put("success", false);
            response.put("data", null);
            response.put("error_message", "Organization not found");
            return ResponseEntity.badRequest().body(response);
        }

        var overview = teamAiOverviewService.getLatestTeamOverview(targetOrg.getId())
            .orElse(null);
        if (overview == null) {
          log.info("[Team AI Overview] No overview found for organization: {}", orgName);
          response.put("success", false);
          response.put("data", null);
          response.put("error_message", "No AI overview found for this team");
        } else {
          log.info("[Team AI Overview] Overview found for organization: {}", orgName);
          response.put("success", true);
          if (overview.getOverviewJson() != null) {
            response.put("data", overview.getOverviewJson());
          } else {
            response.put("data", overview.getOverviewText());
          }
          response.put("error_message", null);
        }
        return ResponseEntity.ok(response);
      }
      Object data = dashboardAnalyticsService.getAnalytics(type, orgName, userEmail, simulationId, userDetails.user());
      Map<String, Object> response = new HashMap<>();
      response.put("type", type);
      response.put("success", true);
      response.put("data", data);
      response.put("error_message", null);
      return ResponseEntity.ok(response);
    } catch (AccessDeniedException e) {
      log.error("[AI Overview] Access denied for analytics request", e);
      Map<String, Object> response = new HashMap<>();
      response.put("type", type);
      response.put("success", false);
      response.put("data", null);
      response.put("error_message", "Access denied");
      return ResponseEntity.status(403).body(response);
    } catch (Exception e) {
      log.error("[AI Overview] Unexpected error for analytics request", e);
      Map<String, Object> response = new HashMap<>();
      response.put("type", type);
      response.put("success", false);
      response.put("data", null);
      response.put("error_message", e.getMessage());
      return ResponseEntity.internalServerError().body(response);
    }
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
  @PostMapping("/analytics/trigger-profile-ai-overview")
  public ResponseEntity<Map<String, Object>> triggerProfileAiOverviewGeneration() {
    Map<String, Object> response = new HashMap<>();
    try {
      LocalDateTime since = LocalDateTime.now().minusHours(6);
      log.info("[AI Overview] Triggering manual overview generation for users with updates since: {}", since);
      List<User> users = userHyperParameterRepository.findUsersWithRecentHyperparamUpdates(since);

      int generated = 0;
      for (User user : users) {
        profileAiOverviewService.generateAndStoreProfileOverview(user);
        generated++;
      }
      response.put("success", true);
      response.put("generated_count", generated);
      response.put("error_message", null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("[AI Overview] Error triggering manual overview generation", e);
      response.put("success", false);
      response.put("generated_count", 0);
      response.put("error_message", e.getMessage());
      return ResponseEntity.internalServerError().body(response);
    }
  }

  @PreAuthorize("hasRole('ROLE_OWNER')")
  @PostMapping("/analytics/trigger-team-ai-overview")
  public ResponseEntity<Map<String, Object>> triggerTeamAiOverviewGeneration(Authentication authentication) {
    Map<String, Object> response = new HashMap<>();
    try {
      log.info("[Team AI Overview] Triggering manual overview generation by owner");
      // For all organizations, force generation (ignore 15-day check), but require at least 1 active user in last 24h
      var organizations = organizationRepository.findAll();
      var cutoffTime = LocalDateTime.now().minusHours(24);
      int generated = 0;
      for (var org : organizations) {
        var orgUsers = userRepository.findAllByOrganization(org);
        long activeUsers = orgUsers.stream()
          .filter(user -> userHyperParameterRepository.hasRecentUpdates(user.getId(), cutoffTime))
          .count();
        if (activeUsers >= 1) {
          var overview = teamAiOverviewService.generateAndStoreTeamOverview(org, true);
          if (overview != null) generated++;
        } else {
          log.info("[Team AI Overview] Skipping organization {} - only {} active users (minimum required: 1)", org.getName(), activeUsers);
        }
      }
      response.put("success", true);
      response.put("generated_count", generated);
      response.put("error_message", null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("[Team AI Overview] Error triggering manual overview generation", e);
      response.put("success", false);
      response.put("generated_count", 0);
      response.put("error_message", e.getMessage());
      return ResponseEntity.internalServerError().body(response);
    }
  }
}
