package com.backend.softtrainer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public record AllSimulationsResponseDto(@JsonProperty("skill_id") Long skillId,
                                       Set<SimulationAvailabilityStatusDto> simulations,
                                       @Deprecated
                                       Set<SimulationAvailabilityStatusDto> names,
                                       boolean success,
                                       @JsonProperty("error_message") String errorMessage) {
}
