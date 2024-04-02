package com.backend.softtrainer.dtos;

import com.backend.softtrainer.dtos.flow.FlowNodeDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlowRequestDto {

  private List<FlowNodeDto> flow;

  //todo extract name of flow there
  private String name;

  private List<CharacterDto> characters;

}
