package com.backend.softtrainer.entities.messages;

import com.backend.softtrainer.entities.Character;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.entities.enums.ChatRole;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

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

  @Column(name = "timestamp", insertable = false, updatable = false)
  @CreationTimestamp(source = SourceType.DB)
  private LocalDateTime timestamp;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @JoinColumn(name = "chat_id")
  private Chat chat;

  @JsonIgnore
  @ManyToOne
  private FlowNode flowNode;

  @Enumerated(EnumType.STRING)
  @JsonProperty("message_type")
  private MessageType messageType;

  @JsonIgnore
  @Enumerated(EnumType.STRING)
  private ChatRole role;

  @ManyToOne
  @JsonProperty("author")
  private Character character;

}
