package com.backend.softtrainer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChatRequestDto {

  @Deprecated
  @JsonProperty("skill_id")
  private Long skillId;

  @JsonProperty("simulation_id")
  private Long simulationId;

}
