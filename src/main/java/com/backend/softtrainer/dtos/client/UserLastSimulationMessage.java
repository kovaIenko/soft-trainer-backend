package com.backend.softtrainer.dtos.client;

import com.backend.softtrainer.dtos.UserHyperParamResponseDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
public class UserLastSimulationMessage extends UserMessageDto {

  @JsonProperty("next_simulation_id")
  private Long nextSimulationId;

  @JsonProperty("achieved_scores")
  List<UserHyperParamResponseDto> hyperParams;

}
