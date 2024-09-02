package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.AvailableSimulationFlowDto;
import com.backend.softtrainer.dtos.SimulationNodesDto;
import com.backend.softtrainer.dtos.SimulationRequestDto;
import com.backend.softtrainer.dtos.SimulationUploadResponseDto;
import com.backend.softtrainer.repositories.SimulationRepository;
import com.backend.softtrainer.services.FlowService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/flow")
@AllArgsConstructor
@Slf4j
public class SimulationsController {

  private final FlowService flowService;
  private final SimulationRepository simulationRepository;

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

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
  @GetMapping("/get")
  public ResponseEntity<AvailableSimulationFlowDto> getAvailableSimulationsWithNodes() {
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
