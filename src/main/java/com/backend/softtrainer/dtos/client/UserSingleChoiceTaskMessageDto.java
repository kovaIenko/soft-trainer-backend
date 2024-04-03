package com.backend.softtrainer.dtos.client;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class UserSingleChoiceTaskMessageDto extends UserMessageDto {

//  @JsonIgnore
//  private String correct;

  private String answer;

  //todo split it into list
  private String options;

}
