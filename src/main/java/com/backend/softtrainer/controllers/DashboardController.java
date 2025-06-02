package com.backend.softtrainer.controllers;

import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.UserHyperParameterRepository;
import com.backend.softtrainer.repositories.UserRepository;
import com.backend.softtrainer.services.analytics.DashboardAnalyticsService;
import com.backend.softtrainer.services.analytics.ProfileAiOverviewService;
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
  private final UserRepository userRepository;
  private final UserHyperParameterRepository userHyperParameterRepository;

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
}
