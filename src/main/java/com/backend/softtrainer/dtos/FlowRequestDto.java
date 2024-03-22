package com.backend.softtrainer.dtos;

import com.backend.softtrainer.dtos.flow.FlowQuestionDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.security.auth.callback.CallbackHandler;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlowRequestDto {

  private List<FlowQuestionDto> flow;

  //todo extract name of flow there
  private String name;

  private List<CharacterDto> characters;

}
