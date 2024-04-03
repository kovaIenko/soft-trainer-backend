package com.backend.softtrainer.entities.flow;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@DiscriminatorValue("2")
public class Text extends FlowNode {

  @Column(length = 700)
  private String text;

}
