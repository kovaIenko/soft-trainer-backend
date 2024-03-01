package com.backend.softtrainer.entities.messages;

import com.backend.softtrainer.entities.MessageType;
import com.backend.softtrainer.entities.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity(name = "messages")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

  @Id
  @JsonIgnore
  private String id;

  private LocalDateTime timestamp;

  private String chatId;

  @JsonIgnore
  private String previousMessageId;

  @Enumerated(EnumType.STRING)
  private MessageType messageType;

  @JsonIgnore
  @Enumerated(EnumType.STRING)
  private Role role;

}
