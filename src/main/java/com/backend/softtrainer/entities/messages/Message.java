package com.backend.softtrainer.entities.messages;

import com.backend.softtrainer.entities.Character;
import com.backend.softtrainer.entities.MessageType;
import com.backend.softtrainer.entities.Role;
import com.backend.softtrainer.entities.flow.FlowQuestion;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
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

  @JsonIgnore
  private Long chatId;

  @JsonIgnore
  @ManyToOne
  private FlowQuestion flowQuestion;

  @Enumerated(EnumType.STRING)
  @JsonProperty("message_type")
  private MessageType messageType;

  @JsonIgnore
  @Enumerated(EnumType.STRING)
  private Role role;

  @ManyToOne
  @JsonProperty("author")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Character character;

  @Transient
  @JsonProperty("is_current_user")
  private boolean isCurrentUser;

}
