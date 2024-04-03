package com.backend.softtrainer.entities.flow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
public class SingleChoiceTask extends FlowNode {

  @Column(length = 700)
  private String correct;

  @Column(length = 700)
  private String options;

}
