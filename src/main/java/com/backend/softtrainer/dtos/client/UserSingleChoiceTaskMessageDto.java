package com.backend.softtrainer.dtos.client;

import com.backend.softtrainer.dtos.MessageAnswerOptionDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
public class UserSingleChoiceTaskMessageDto extends UserMessageDto {

//  @JsonIgnore
//  private String correct;

  private String answer;

  @JsonProperty("is_voted")
  private boolean isVoted;

  private List<MessageAnswerOptionDto> options;

}
