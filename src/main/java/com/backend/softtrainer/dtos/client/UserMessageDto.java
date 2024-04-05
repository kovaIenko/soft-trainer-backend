package com.backend.softtrainer.dtos.client;


import com.backend.softtrainer.entities.Character;
import com.backend.softtrainer.entities.MessageType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * combined backend messages question and answers into one common dto
 */
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserMessageDto {

  @JsonProperty("timestamp")
  private LocalDateTime timestamp;

  @JsonIgnore
  private Long chatId;

  @JsonProperty("message_type")
  private MessageType messageType;

  @JsonProperty("author")
  private Character character;

}
