package com.backend.softtrainer.entities.flow;

import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@NoArgsConstructor
public class MultipleChoiceQuestion extends FlowQuestion {

  private String correct;

  private String options;

}
