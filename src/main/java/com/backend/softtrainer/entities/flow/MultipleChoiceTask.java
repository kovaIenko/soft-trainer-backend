package com.backend.softtrainer.entities.flow;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@NoArgsConstructor
@Data
@DiscriminatorValue("4")
public class MultipleChoiceTask extends FlowNode {

  @Column
  private String correct;

  @Column(length = 700)
  private String options;

}
