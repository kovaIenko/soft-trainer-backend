package com.backend.softtrainer.entities.messages;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@NoArgsConstructor
@SuperBuilder
public class MultiChoiceTaskAnswerMessage extends Message{

  @Column(length = 700)
  private String answer;

  @Column(length = 700)
  private String options;

  @Column(length = 700)
  private String correct;

}
