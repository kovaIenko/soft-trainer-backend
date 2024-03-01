package com.backend.softtrainer.entities.messages;

import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@Data
public class MultiChoiceAnswerMessage extends Message {

  private String question;

  private String options;

}
