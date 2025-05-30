package com.backend.softtrainer.entities.flow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SingleChoiceQuestion extends FlowNode {

  @Column(length = 700)
  private String correct;

  //todo split it into list
  @Column(length = 700)
  private String options;

}
