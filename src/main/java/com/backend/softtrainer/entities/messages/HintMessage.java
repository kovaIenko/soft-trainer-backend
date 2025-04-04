package com.backend.softtrainer.entities.messages;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
public class HintMessage extends Message {

  @Column(length = 2000)
  private String content;

  @Column(name = "based_message_id")
  private String basedMessageId;

  private String title;

}
