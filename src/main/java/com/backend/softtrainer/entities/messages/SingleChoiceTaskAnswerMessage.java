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
public class SingleChoiceTaskAnswerMessage extends Message {

  @Column(length = 700)
  private String correct;

  @Column(length = 700)
  private String answer;

  //todo split it into list
  @Column(length = 700)
  private String options;

}
