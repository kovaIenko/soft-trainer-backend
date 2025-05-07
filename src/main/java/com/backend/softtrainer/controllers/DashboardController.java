package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.AvailableSimulationFlowDto;
import com.backend.softtrainer.dtos.SimulationNodesDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@AllArgsConstructor
@Slf4j
public class DashboardController {

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

}
