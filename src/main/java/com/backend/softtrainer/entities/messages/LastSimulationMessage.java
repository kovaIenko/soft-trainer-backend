package com.backend.softtrainer.entities.messages;

import com.backend.softtrainer.dtos.UserHyperParamResponseDto;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@NoArgsConstructor
@SuperBuilder
public class LastSimulationMessage extends Message {

  private Long nextSimulationId;

  List<UserHyperParamResponseDto> hyperParams;

}
