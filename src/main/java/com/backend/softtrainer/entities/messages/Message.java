package com.backend.softtrainer.entities.messages;

import com.backend.softtrainer.entities.MessageType;
import com.backend.softtrainer.entities.Role;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
  private String id;

  private LocalDateTime timestamp;

  private String chatId;

  private String previousMessageId;

  private MessageType messageType;

  private Role role;

}
