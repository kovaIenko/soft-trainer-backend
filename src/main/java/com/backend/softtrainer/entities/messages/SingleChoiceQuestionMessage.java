package com.backend.softtrainer.entities.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@NoArgsConstructor
@SuperBuilder
public class SingleChoiceQuestionMessage extends Message {

  @JsonIgnore
  private String correct;

  //todo split it into list
  private String options;

}
