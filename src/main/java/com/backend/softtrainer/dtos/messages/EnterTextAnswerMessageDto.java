package com.backend.softtrainer.dtos.messages;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


@Data
public class EnterTextAnswerMessageDto extends MessageRequestDto {

  private String content;

}
