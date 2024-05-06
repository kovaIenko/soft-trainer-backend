package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.SimulationRequestDto;
import com.backend.softtrainer.dtos.SimulationUploadResponseDto;
import com.backend.softtrainer.services.FlowService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
  @PutMapping("/upload")
  public ResponseEntity<SimulationUploadResponseDto> upload(@RequestBody final SimulationRequestDto simulationRequestDto) {
    try {
      flowService.uploadFlow(simulationRequestDto);
      var statusMessage = String.format(
        "Successful stored skill %s and simulation  %s",
        simulationRequestDto.getSkill().name(),
        simulationRequestDto.getName()
      );
      log.info(statusMessage);
      return ResponseEntity.ok(new SimulationUploadResponseDto(simulationRequestDto.getName(), true, statusMessage));
    } catch (NoSuchElementException e) {
      return ResponseEntity.ok(new SimulationUploadResponseDto("", false, e.getMessage()));
    }
  }

}
