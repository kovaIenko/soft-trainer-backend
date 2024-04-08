package com.backend.softtrainer.entities.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
public class SingleChoiceTaskQuestionMessage extends Message {

  @JsonIgnore
  @Column(length = 700)
  private String correct;

  //todo split it into list
  @Column(length = 700)
  private String options;

}
