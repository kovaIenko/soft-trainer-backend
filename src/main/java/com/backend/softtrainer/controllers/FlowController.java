package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.FlowRequestDto;
import com.backend.softtrainer.dtos.FlowResponseDto;
import com.backend.softtrainer.dtos.AllFlowsResponseDto;
import com.backend.softtrainer.services.FlowService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/flow")
@AllArgsConstructor
@Slf4j
public class FlowController {

  private final FlowService flowService;

  @PutMapping("/upload")
  public ResponseEntity<FlowResponseDto> upload(@RequestBody final FlowRequestDto flowRequestDto) {

    if (flowService.existsByName(flowRequestDto.getName())) {
      return ResponseEntity.ok(new FlowResponseDto(
        flowRequestDto.getName(),
        false,
        String.format(
          "The flow with such name %s already exists",
          flowRequestDto.getName()
        )
      ));
    }

    flowService.uploadFlow(flowRequestDto);
    var statusMessage = String.format("Successful stored flow with name %s", flowRequestDto.getName());
    log.info(statusMessage);
    return ResponseEntity.ok(new FlowResponseDto(flowRequestDto.getName(), true, statusMessage));
  }

  @GetMapping("/names")
  public ResponseEntity<AllFlowsResponseDto> getAllFlowNames() {
    var names = flowService.getAllNameFlows();
    return ResponseEntity.ok(new AllFlowsResponseDto(names, true));
  }

}
