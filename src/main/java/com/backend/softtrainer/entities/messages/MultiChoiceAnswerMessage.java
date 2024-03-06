package com.backend.softtrainer.entities.messages;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.awt.List;

@Entity
@Data
@NoArgsConstructor
@SuperBuilder
public class MultiChoiceAnswerMessage extends Message{

  private String answer;

}
