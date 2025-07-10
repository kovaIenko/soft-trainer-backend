package com.backend.softtrainer.dtos.flow;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SingleChoiceQuestionDto extends FlowNodeDto {

  @JsonProperty("correct_answer_position")
  private String correct;

  private List<OptionDto> options;

}
