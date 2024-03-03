package com.backend.softtrainer.dtos.messages;

import lombok.Data;

@Data
public class SingleChoiceAnswerMessageDto extends MessageRequestDto {

  private String answer;

}
