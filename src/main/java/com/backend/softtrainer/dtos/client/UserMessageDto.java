package com.backend.softtrainer.dtos.client;


import com.backend.softtrainer.entities.Character;
import com.backend.softtrainer.entities.enums.ChatRole;
import com.backend.softtrainer.entities.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
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

  private String id;

  @Column(name = "id_temp")
  private String idTemp;

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

  @JsonProperty("has_hint")
  private boolean hasHint;

  @JsonProperty("hint_message")
  private UserHintMessageDto hintMessage;

}
