package com.backend.softtrainer.entities.messages;

import com.backend.softtrainer.dtos.UserHyperParamResponseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class LastSimulationMessage extends Message {

  private Long nextSimulationId;

  private List<UserHyperParamResponseDto> hyperParams;

  private String aiSummary;

}
