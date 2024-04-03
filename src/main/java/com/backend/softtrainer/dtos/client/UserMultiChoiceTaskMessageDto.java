package com.backend.softtrainer.dtos.client;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class UserMultiChoiceTaskMessageDto extends UserMessageDto{

  private String answer;

  //todo split it into list
  private String options;

}
