package com.backend.softtrainer.entities.messages;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Entity
@SuperBuilder
@NoArgsConstructor
@Data
public class EnterTextQuestionMessage extends Message {

  @Column(length = 700)
  private String content;

  private String answer;

  private String openAnswer;

  @Column(length = 700)
  private String correct;

  @Column(length = 700)
  private String options;

}
