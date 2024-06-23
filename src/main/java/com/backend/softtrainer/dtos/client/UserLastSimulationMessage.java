package com.backend.softtrainer.dtos.client;

import com.backend.softtrainer.dtos.UserHyperParamResponseDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserLastSimulationMessage extends UserMessageDto {

  @Deprecated
  @JsonProperty("next_simulation_id")
  private Long nextSimulationId;

  @JsonProperty("achieved_scores")
  private List<UserHyperParamResponseDto> hyperParams;

  private String description;

  private String title;

}
