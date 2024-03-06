package com.backend.softtrainer.entities.messages;

import com.backend.softtrainer.entities.MessageType;
import com.backend.softtrainer.entities.Role;
import com.backend.softtrainer.entities.flow.FlowQuestion;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
  @ManyToOne
  private FlowQuestion flowQuestion;

  @Enumerated(EnumType.STRING)
  private MessageType messageType;

  @JsonIgnore
  @Enumerated(EnumType.STRING)
  private Role role;

}
