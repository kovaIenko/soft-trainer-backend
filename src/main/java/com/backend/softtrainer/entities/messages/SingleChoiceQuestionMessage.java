package com.backend.softtrainer.entities.messages;

import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@Data
public class SingleChoiceQuestionMessage extends Message {

  private String question;

  private String correct;

  //todo split it into list
  private String options;

}
