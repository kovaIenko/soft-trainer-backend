package com.backend.softtrainer.dtos;

import com.backend.softtrainer.dtos.flow.FlowNodeDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimulationRequestDto {

  private List<FlowNodeDto> flow;

  private String name;

  private Double hearts;

  private List<CharacterDto> characters;

  private List<HyperParameterDto> hyperparameters;

  private SkillRequestDto skill;

}
