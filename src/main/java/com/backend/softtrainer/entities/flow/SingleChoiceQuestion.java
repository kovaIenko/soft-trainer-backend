package com.backend.softtrainer.entities.flow;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
public class SingleChoiceQuestion extends FlowQuestion {

  private String correct;

  //todo split it into list
  private String options;

}
