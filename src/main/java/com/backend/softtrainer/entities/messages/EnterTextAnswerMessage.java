package com.backend.softtrainer.entities.messages;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
public class EnterTextAnswerMessage extends Message {

  private String content;

}
