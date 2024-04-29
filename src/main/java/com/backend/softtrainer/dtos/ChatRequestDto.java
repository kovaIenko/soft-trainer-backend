package com.backend.softtrainer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChatRequestDto {

  @JsonProperty("owner_id")
  private Long ownerId;

  @JsonProperty("skill_id")
  private Long skillId;

  @JsonProperty("simulation_name")
  private String simulationName;

}
