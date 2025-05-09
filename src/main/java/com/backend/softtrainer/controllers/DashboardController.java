package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.analytics.AnalyticsResponseDto;
import com.backend.softtrainer.services.analytics.DashboardAnalyticsService;
import com.backend.softtrainer.services.auth.CustomUsrDetails;
import com.backend.softtrainer.services.auth.CustomUsrDetailsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@AllArgsConstructor
@Slf4j
public class DashboardController {

  private final CustomUsrDetailsService usrDetailsService;
  private final DashboardAnalyticsService dashboardAnalyticsService;

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
  @GetMapping("/analytics")
  public ResponseEntity<AnalyticsResponseDto> getAnalytics(
    @RequestParam String type,
    @RequestParam(required = false) String orgName,
    @RequestParam(required = false) String userEmail,
    @RequestParam(required = false) Long simulationId,
    Authentication authentication
  ) {
    var userDetails = (CustomUsrDetails) usrDetailsService.loadUserByUsername(authentication.getName());

    try {
      Object data = dashboardAnalyticsService.getAnalytics(type, orgName, userEmail, simulationId, userDetails.user());
      return ResponseEntity.ok(new AnalyticsResponseDto(type, data));
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(403).build();
    }
  }

}
