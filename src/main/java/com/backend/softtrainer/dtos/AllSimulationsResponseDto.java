package com.backend.softtrainer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public record AllSimulationsResponseDto(@JsonProperty("skill_id") Long skillId,
                                       Set<SimulationAvailabilityStatusDto> simulations,
                                       boolean success,
                                       @JsonProperty("error_message") String errorMessage) {
}
