package com.backend.softtrainer.dtos.client;

import com.backend.softtrainer.dtos.MessageAnswerOptionDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
@SuperBuilder
@NoArgsConstructor
public class UserMultiChoiceTaskMessageDto extends UserMessageDto {

  private String answer;

  @JsonProperty("is_voted")
  private boolean isVoted;

  private List<MessageAnswerOptionDto> options;

  private CorrectnessState correctness;

}
