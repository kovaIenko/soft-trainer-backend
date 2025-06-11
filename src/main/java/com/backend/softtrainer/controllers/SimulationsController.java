package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.AvailableSimulationFlowDto;
import com.backend.softtrainer.dtos.SimulationNodesDto;
import com.backend.softtrainer.dtos.SimulationRequestDto;
import com.backend.softtrainer.dtos.SimulationUploadResponseDto;
import com.backend.softtrainer.entities.Organization;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.repositories.OrganizationRepository;
import com.backend.softtrainer.repositories.SimulationRepository;
import com.backend.softtrainer.services.FlowService;
import com.backend.softtrainer.services.auth.CustomUsrDetails;
import com.backend.softtrainer.services.auth.CustomUsrDetailsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/flow")
@AllArgsConstructor
@Slf4j
public class SimulationsController {

  private final FlowService flowService;
  private final SimulationRepository simulationRepository;
  private final OrganizationRepository organizationRepository;
  private final CustomUsrDetailsService customUsrDetailsService;

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
  @PutMapping("/upload")
  public ResponseEntity<SimulationUploadResponseDto> upload(@RequestBody final SimulationRequestDto simulationRequestDto) {
    try {
      flowService.uploadFlow(simulationRequestDto);
      var statusMessage = String.format(
        "Successful stored skill %s and simulation %s",
        simulationRequestDto.getSkill().skillId(),
        simulationRequestDto.getName()
      );
      log.info(statusMessage);
      return ResponseEntity.ok(new SimulationUploadResponseDto(simulationRequestDto.getName(), true, statusMessage));
    } catch (NoSuchElementException e) {
      return ResponseEntity.ok(new SimulationUploadResponseDto("", false, e.getMessage()));
    }
  }

  @PreAuthorize("hasRole('ROLE_OWNER')")
  @GetMapping("/get")
  public ResponseEntity<AvailableSimulationFlowDto> getAllSimulations() {
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
  @GetMapping("/get/org")
  public ResponseEntity<AvailableSimulationFlowDto> getOrganizationSimulations(
    @RequestParam(name = "org_name") String orgName,
    Authentication authentication
  ) {
    try {
      var userDetails = (CustomUsrDetails) customUsrDetailsService.loadUserByUsername(authentication.getName());
      boolean isOwner = userDetails.getAuthorities().stream()
        .anyMatch(auth -> auth.getAuthority().equals("ROLE_OWNER"));

      // If not owner, verify admin belongs to the organization
      if (!isOwner && !customUsrDetailsService.orgHasEmployee(authentication, orgName)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(new AvailableSimulationFlowDto(null, false, "You don't have access to this organization"));
      }

      // Get organization's available skills
      Organization org = organizationRepository.findByName(orgName)
        .orElseThrow(() -> new NoSuchElementException("Organization not found"));

      // Get all simulations for the organization's skills
      var openSimulations = org.getAvailableSkills().stream()
        .flatMap(skill -> skill.getSimulations().keySet().stream())
        .filter(Simulation::isOpen)
        .collect(Collectors.toList());

      // Load flow nodes for each simulation
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

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<AvailableSimulationFlowDto> handleAccessDeniedException(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
      .body(new AvailableSimulationFlowDto(null, false, "Access denied: " + ex.getMessage()));
  }
}
