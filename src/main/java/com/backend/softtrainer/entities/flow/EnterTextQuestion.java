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
public class EnterTextQuestion extends FlowNode {

  //use %s to exchange data inside of the prompt
  @Column(length = 1500)
  private String prompt;

}
