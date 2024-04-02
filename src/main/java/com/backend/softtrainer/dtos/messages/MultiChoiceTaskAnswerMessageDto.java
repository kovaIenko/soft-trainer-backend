package com.backend.softtrainer.dtos.messages;

import lombok.Data;

@Data
public class MultiChoiceTaskAnswerMessageDto extends MessageRequestDto {

  private String answer;

  private String options;

  private String correct;

}
