package com.backend.softtrainer.entities.flow;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class ResultSimulationNode extends FlowNode {

  private String prompt;

}
