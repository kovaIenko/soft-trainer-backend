package com.backend.softtrainer.dtos.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class UserEnterTextMessageDto extends UserMessageDto {

  private String content;

  @JsonProperty("is_voted")
  private boolean isVoted;

  private CorrectnessState correctness;

}
