package com.backend.softtrainer.dtos.flow;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnterTextQuestionDto extends FlowNodeDto {

  //use %s to exchange data inside the prompt
  private String prompt;

  @JsonProperty("correct_answer_position")
  private String correct;

  private List<String> options;

}
