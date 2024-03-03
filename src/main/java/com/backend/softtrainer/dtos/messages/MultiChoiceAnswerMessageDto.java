package com.backend.softtrainer.dtos.messages;

import lombok.Data;

@Data
public class MultiChoiceAnswerMessageDto extends MessageRequestDto {

  private String answer;

}