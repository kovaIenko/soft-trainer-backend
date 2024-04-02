package com.backend.softtrainer.dtos.messages;

import lombok.Data;

@Data
public class SingleChoiceTaskAnswerMessageDto extends MessageRequestDto {

  private String answer;

  private String correct;

  //todo split it into list
  private String options;

}
