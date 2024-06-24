package com.backend.softtrainer.dtos.messages;


import lombok.Data;


@Data
public class EnterTextAnswerMessageDto extends MessageRequestDto {

  private String answer;

}
