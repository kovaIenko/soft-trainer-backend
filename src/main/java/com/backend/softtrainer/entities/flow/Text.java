package com.backend.softtrainer.entities.flow;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
public class Text extends FlowQuestion {
  private String text;
}
