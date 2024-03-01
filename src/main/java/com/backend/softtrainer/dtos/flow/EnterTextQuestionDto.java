package com.backend.softtrainer.dtos.flow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnterTextQuestionDto extends FlowQuestionDto {

  //use %s to exchange data inside of the prompt
  private String prompt;

}
