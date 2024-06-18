package com.backend.softtrainer.dtos.client;


import com.backend.softtrainer.entities.Character;
import com.backend.softtrainer.entities.enums.ChatRole;
import com.backend.softtrainer.entities.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * combined backend messages question and answers into one common dto
 */
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserMessageDto {

  @JsonProperty("timestamp")
  @JsonIgnore
  private LocalDateTime timestamp;

  @JsonIgnore
  private Long chatId;

  @JsonProperty("message_type")
  private MessageType messageType;

  @JsonProperty("author")
  private Character character;

  @JsonIgnore
  @Enumerated(EnumType.STRING)
  private ChatRole role;

}
