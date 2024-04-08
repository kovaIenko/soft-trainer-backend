package com.backend.softtrainer.entities.messages;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
public class EnterTextMessage extends Message {

  @Column(length = 700)
  private String content;

}
